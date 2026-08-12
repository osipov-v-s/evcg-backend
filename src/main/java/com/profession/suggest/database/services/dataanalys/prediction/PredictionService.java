package com.profession.suggest.database.services.dataanalys.prediction;

import com.profession.suggest.database.entities.auth.Account;
import com.profession.suggest.database.entities.dataanalys.prediction.Prediction;
import com.profession.suggest.database.entities.dataanalys.prediction.PredictionType;
import com.profession.suggest.database.entities.dataanalys.prediction.PredictionTypeEnum;
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
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PredictionService {
    private final PredictionRepository repository;
    private final PredictionMapper mapper;
    private final PredictionTypeService predictionTypeService;
    private final PupilService pupilService;
    private final PsychTestService psychTestService;
    private final ProfessionService professionService;
    private final SpecialistService specialistService;
    private final RestTemplate restTemplate;

    @Value("${prediction.service.url:http://127.0.0.1:8000/predict}")
    private String predictUrl;

    public List<PredictionDTO> getPredictionsByPupilId(Long pupilId) {
        return repository.findByPupilId(pupilId).stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public PredictionResponse getLatestPredictionByAccountId(Long accountId) {
        Pupil pupil = pupilService.getPupilByAccountId(accountId);
        Prediction prediction = repository.findTopByPupilIdOrderByCreatedAtDesc(pupil.getId())
                .orElseThrow(() -> new RuntimeException("No prediction found"));
        return toResponse(prediction);
    }

    public PredictionResponse predictByAccount(Account account) {
        if (account == null || account.getPupil() == null) {
            throw new PredictionIntegrationException(
                    "PREDICTION_ACCOUNT_INVALID",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Prediction is available only for a pupil account");
        }

        Pupil pupil = account.getPupil();
        PredictionRequest request = new PredictionRequest(
                pupil.getId(),
                psychTestService.getAccountRecentTests(account));
        PredictionResponse externalResponse = requestPrediction(request);
        validateExternalResponse(externalResponse, pupil.getId());
        Prediction saved = savePrediction(externalResponse, pupil);
        return toResponse(saved);
    }

    private PredictionResponse requestPrediction(PredictionRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PredictionRequest> requestEntity = new HttpEntity<>(request, headers);
        log.info("Calling prediction service pupilId={}", request.getPupilId());
        try {
            ResponseEntity<PredictionResponse> response = restTemplate.exchange(
                    predictUrl,
                    HttpMethod.POST,
                    requestEntity,
                    PredictionResponse.class);
            if (response.getBody() == null) {
                throw invalidResponse("Prediction service returned an empty response", null);
            }
            return response.getBody();
        } catch (PredictionIntegrationException exception) {
            throw exception;
        } catch (HttpClientErrorException exception) {
            throw new PredictionIntegrationException(
                    "PREDICTION_REQUEST_REJECTED",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Available test results are not sufficient for prediction",
                    exception);
        } catch (HttpServerErrorException exception) {
            throw new PredictionIntegrationException(
                    "PREDICTION_SERVICE_ERROR",
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Prediction service is temporarily unavailable",
                    exception);
        } catch (ResourceAccessException exception) {
            if (isTimeout(exception)) {
                throw new PredictionIntegrationException(
                        "PREDICTION_TIMEOUT",
                        HttpStatus.GATEWAY_TIMEOUT,
                        "Prediction took too long. Please try again later",
                        exception);
            }
            throw new PredictionIntegrationException(
                    "PREDICTION_SERVICE_UNAVAILABLE",
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Prediction service is temporarily unavailable",
                    exception);
        } catch (RestClientException exception) {
            throw new PredictionIntegrationException(
                    "PREDICTION_SERVICE_UNAVAILABLE",
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Prediction service is temporarily unavailable",
                    exception);
        }
    }

    private void validateExternalResponse(PredictionResponse response, Long expectedPupilId) {
        if (response == null
                || response.getPupilId() != expectedPupilId
                || response.getCluster() < 0
                || response.getNearestSpecialistId() <= 0
                || !Double.isFinite(response.getDistance())
                || response.getDistance() < 0
                || response.getPredictedProfession() == null
                || response.getPredictedProfession().isBlank()
                || response.getConfidenceCategory() == null
                || response.getConfidenceCategory().isBlank()) {
            throw invalidResponse("Prediction service returned an invalid response", null);
        }
    }

    private Prediction savePrediction(PredictionResponse response, Pupil pupil) {
        try {
            Profession profession = professionService.getProfessionByName(response.getPredictedProfession());
            Specialist specialist = specialistService.getSpecialistById(response.getNearestSpecialistId());
            PredictionType predictionType = predictionTypeService.getByName(PredictionTypeEnum.CLUSTER);
            if (profession == null || specialist == null || predictionType == null) {
                throw invalidResponse("Prediction response references missing domain data", null);
            }
            Prediction prediction = Prediction.builder()
                    .pupil(pupil)
                    .predictedProfession(profession)
                    .nearestSpecialist(specialist)
                    .predictionType(predictionType)
                    .cluster(response.getCluster())
                    .distance(response.getDistance())
                    .confidenceCategory(response.getConfidenceCategory())
                    .build();
            return repository.save(prediction);
        } catch (PredictionIntegrationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalidResponse("Prediction response references missing domain data", exception);
        }
    }

    private PredictionResponse toResponse(Prediction prediction) {
        return PredictionResponse.builder()
                .pupilId(prediction.getPupil().getId())
                .cluster(prediction.getCluster())
                .predictedProfession(prediction.getPredictedProfession().getName())
                .nearestSpecialistId(prediction.getNearestSpecialist().getId())
                .distance(prediction.getDistance())
                .confidenceCategory(prediction.getConfidenceCategory())
                .createdAt(prediction.getCreatedAt())
                .build();
    }

    private PredictionIntegrationException invalidResponse(String logMessage, Throwable cause) {
        log.warn(logMessage, cause);
        return new PredictionIntegrationException(
                "PREDICTION_INVALID_RESPONSE",
                HttpStatus.BAD_GATEWAY,
                "Prediction service returned an invalid response",
                cause);
    }

    private boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException) return true;
            current = current.getCause();
        }
        return false;
    }
}
