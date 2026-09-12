package com.profession.suggest.database.services.dataanalys.prediction;

import com.profession.suggest.configuration.properties.PredictionProperties;
import com.profession.suggest.database.entities.auth.Account;
import com.profession.suggest.database.entities.dataanalys.prediction.Prediction;
import com.profession.suggest.database.entities.dataanalys.prediction.PredictionType;
import com.profession.suggest.database.entities.dataanalys.prediction.PredictionTypeEnum;
import com.profession.suggest.database.entities.dataanalys.prediction.math.MathPrediction;
import com.profession.suggest.database.entities.professions.Profession;
import com.profession.suggest.database.entities.users.pupil.Pupil;
import com.profession.suggest.database.entities.users.specialist.Specialist;
import com.profession.suggest.database.repositories.dataanalys.prediction.PredictionRepository;
import com.profession.suggest.database.services.dataanalys.psychtests.PsychTestService;
import com.profession.suggest.database.services.profession.ProfessionService;
import com.profession.suggest.database.services.pupil.PupilService;
import com.profession.suggest.database.services.specialist.SpecialistService;
import com.profession.suggest.dto.dataanalys.prediction.PredictionDTO;
import com.profession.suggest.dto.dataanalys.prediction.PredictionMapper;
import com.profession.suggest.dto.dataanalys.prediction.PredictionRequest;
import com.profession.suggest.dto.dataanalys.prediction.PredictionResponse;
import com.profession.suggest.exceptions.PredictionIntegrationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

import com.profession.suggest.configuration.properties.PredictionProperties;
import com.profession.suggest.database.entities.auth.Account;
import com.profession.suggest.database.entities.dataanalys.prediction.PredictionTypeEnum;
import com.profession.suggest.database.entities.users.pupil.Pupil;
import com.profession.suggest.dto.dataanalys.prediction.PredictionRequest;
import com.profession.suggest.exceptions.PredictionIntegrationException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PredictionService {

    private final PredictionProperties predictionProperties;
    private final List<PredictionStrategy<?, ?>> strategies;

    private Map<PredictionTypeEnum, PredictionStrategy<?, ?>> strategyMap;

    @PostConstruct
    void init() {
        strategyMap = strategies.stream()
                .collect(Collectors.toMap(PredictionStrategy::type, Function.identity()));
        log.info("Registered prediction strategies: {}", strategyMap.keySet());

        Set<PredictionTypeEnum> missing = EnumSet.allOf(PredictionTypeEnum.class);
        missing.removeAll(strategyMap.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Missing PredictionStrategy for: " + missing);
        }
    }

    /** Legacy entry defaults to CLUSTER. */
    public Prediction predictByAccount(Account account) {
        return predictByAccount(account, PredictionTypeEnum.CLUSTER);
    }

    /** Typed convenience overloads, so callers don't deal with generics. */
    public Prediction predictCluster(Account account) {
        return predictByAccount(account, PredictionTypeEnum.CLUSTER);
    }

    public MathPrediction predictMath(Account account) {
        return predictByAccount(account, PredictionTypeEnum.MATH);
    }

    /**
     * Generic dispatch returns whatever the strategy persisted.
     * Callers usually prefer the typed overloads above.
     */
    @SuppressWarnings("unchecked")
    public <R, P> P predictByAccount(Account account, PredictionTypeEnum type) {
        if (account == null || account.getPupil() == null) {
            throw new PredictionIntegrationException(
                    "PREDICTION_ACCOUNT_INVALID",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Prediction is available only for a pupil account");
        }

        String url = predictionProperties.getUrls().get(type);
        if (url == null || url.isBlank()) {
            throw new PredictionIntegrationException(
                    "PREDICTION_TYPE_UNSUPPORTED",
                    HttpStatus.BAD_REQUEST,
                    "No URL configured for prediction type: " + type);
        }

        PredictionStrategy<Object, Object> strategy =
                (PredictionStrategy<Object, Object>) strategyMap.get(type);
        if (strategy == null) {
            throw new PredictionIntegrationException(
                    "PREDICTION_TYPE_UNSUPPORTED",
                    HttpStatus.BAD_REQUEST,
                    "No strategy registered for prediction type: " + type);
        }

        Pupil pupil = account.getPupil();
        PredictionRequest request = strategy.buildRequest(account, pupil);
        Object external = callExternal(strategy, request, url);
        strategy.validate(external, pupil.getId());
        return (P) strategy.save(external, pupil);
    }

    // ------------------------------------------------------------------
    // HTTP error translation strategies only see domain exceptions
    // ------------------------------------------------------------------

    private <R> R callExternal(PredictionStrategy<R, ?> strategy,
                               PredictionRequest request,
                               String url) {
        try {
            return strategy.call(request, url);
        } catch (PredictionIntegrationException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            throw new PredictionIntegrationException(
                    "PREDICTION_REQUEST_REJECTED",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Available test results are not sufficient for prediction",
                    e);
        } catch (HttpServerErrorException e) {
            throw new PredictionIntegrationException(
                    "PREDICTION_SERVICE_ERROR",
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Prediction service is temporarily unavailable",
                    e);
        } catch (ResourceAccessException e) {
            if (isTimeout(e)) {
                throw new PredictionIntegrationException(
                        "PREDICTION_TIMEOUT",
                        HttpStatus.GATEWAY_TIMEOUT,
                        "Prediction took too long. Please try again later",
                        e);
            }
            throw new PredictionIntegrationException(
                    "PREDICTION_SERVICE_UNAVAILABLE",
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Prediction service is temporarily unavailable",
                    e);
        } catch (RestClientException e) {
            throw new PredictionIntegrationException(
                    "PREDICTION_SERVICE_UNAVAILABLE",
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Prediction service is temporarily unavailable",
                    e);
        }
    }

    private boolean isTimeout(Throwable throwable) {
        for (Throwable c = throwable; c != null; c = c.getCause()) {
            if (c instanceof SocketTimeoutException) return true;
        }
        return false;
    }
}
