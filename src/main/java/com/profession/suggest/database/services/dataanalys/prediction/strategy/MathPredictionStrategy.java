package com.profession.suggest.database.services.dataanalys.prediction.strategy;

import com.profession.suggest.database.entities.auth.Account;
import com.profession.suggest.database.entities.dataanalys.prediction.PredictionType;
import com.profession.suggest.database.entities.dataanalys.prediction.PredictionTypeEnum;
import com.profession.suggest.database.entities.dataanalys.prediction.math.MathPrediction;
import com.profession.suggest.database.entities.users.pupil.Pupil;
import com.profession.suggest.database.repositories.dataanalys.prediction.MathPredictionRepository;
import com.profession.suggest.database.services.dataanalys.prediction.PredictionStrategy;
import com.profession.suggest.database.services.dataanalys.prediction.PredictionTypeService;
import com.profession.suggest.database.services.dataanalys.psychtests.PsychTestService;

import com.profession.suggest.dto.dataanalys.prediction.PredictionRequest;
import com.profession.suggest.dto.dataanalys.prediction.math.MathPredictionResponse;
import com.profession.suggest.exceptions.PredictionIntegrationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.Period;

@Component
@RequiredArgsConstructor
@Slf4j
public class MathPredictionStrategy implements PredictionStrategy<MathPredictionResponse, MathPrediction>{
    private final RestTemplate restTemplate;
    private final PsychTestService psychTestService;
    private final PredictionTypeService predictionTypeService;
    private final MathPredictionRepository mathPredictionRepository;

    @Override
    public PredictionTypeEnum type() {
        return PredictionTypeEnum.MATH;
    }

    @Override
    public PredictionRequest buildRequest(Account account, Pupil pupil) {
        // Same payload for now; change here when math needs different input.
        return new PredictionRequest(
                pupil.getId(),
                pupil.getBirthday() != null
                        ? Period.between(pupil.getBirthday(), LocalDate.now()).getYears()
                        : 14,
                psychTestService.getAccountRecentTests(account));
    }

    @Override
    public MathPredictionResponse call(PredictionRequest request, String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PredictionRequest> entity = new HttpEntity<>(request, headers);

        log.info("MATH call pupilId={} url={}", request.getPupilId(), url);

        ResponseEntity<MathPredictionResponse> response =
                restTemplate.exchange(url, HttpMethod.POST, entity, MathPredictionResponse.class);

        if (response.getBody() == null) {
            throw invalidResponse("Math prediction service returned an empty response");
        }
        return response.getBody();
    }

    @Override
    public void validate(MathPredictionResponse r, Long expectedPupilId) {
        if (r == null
                || !expectedPupilId.equals(r.getPupilId())
                || r.getPercentage() == null || r.getPercentage() < 0 || r.getPercentage() > 100
                || r.getRecommendation() == null || r.getRecommendation().isBlank()
                || r.getFinalScore() == null
                || r.getUtility() == null
                || r.getAizenNorm() == null
                || r.getBelbinNorm() == null
                || r.getBennetNorm() == null) {
            throw invalidResponse("Math prediction service returned an invalid response");
        }
    }

    @Override
    public MathPrediction save(MathPredictionResponse r, Pupil pupil) {
        PredictionType type = predictionTypeService.getByName(PredictionTypeEnum.MATH);
        if (type == null) {
            throw invalidResponse("PredictionType MATH row is missing in DB");
        }

        return mathPredictionRepository.save(MathPrediction.builder()
                .pupil(pupil)
                .predictionType(type)
                .percentage(r.getPercentage())
                .recommendation(r.getRecommendation())
                .aizenNorm(r.getAizenNorm())
                .belbinNorm(r.getBelbinNorm())
                .bennetNorm(r.getBennetNorm())
                .finalScore(r.getFinalScore())
                .utility(r.getUtility())
                .build());
    }

    private PredictionIntegrationException invalidResponse(String message) {
        log.warn(message);
        return new PredictionIntegrationException(
                "PREDICTION_INVALID_RESPONSE",
                HttpStatus.BAD_GATEWAY,
                "Prediction service returned an invalid response");
    }
}
