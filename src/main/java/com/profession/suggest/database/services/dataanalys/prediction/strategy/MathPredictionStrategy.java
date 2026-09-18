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
import com.profession.suggest.dto.dataanalys.prediction.math.MathPredictionDTO;
import com.profession.suggest.dto.dataanalys.prediction.math.MathPredictionResponse;
import com.profession.suggest.exceptions.PredictionIntegrationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class MathPredictionStrategy implements PredictionStrategy<MathPredictionResponse, List<MathPredictionDTO>>{
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
        if (r == null || !expectedPupilId.equals(r.pupilId())) {
            throw invalidResponse("Math prediction service returned an invalid response");
        }

        List<MathPredictionResponse.PredictedProfession> predictions = r.professions();
        if (predictions == null || predictions.isEmpty()) {
            throw invalidResponse("Math prediction service returned no predictions");
        }

        for (MathPredictionResponse.PredictedProfession p: predictions) {
            if (p == null
                    || p.percentage() == null || p.percentage() < 0 || p.percentage() > 100
                    || p.recommendation() == null || p.recommendation().isBlank()
                    || p.profession() == null || p.profession().isBlank()
                    || p.finalScore() == null
                    || p.utility() == null
                    || p.aizenNorm() == null
                    || p.belbinNorm() == null
                    || p.bennetNorm() == null) {
                throw invalidResponse(
                        "Math prediction service returned an invalid response ");
            }
        }
    }

    @Override
    public List<MathPredictionDTO> save(MathPredictionResponse r, Pupil pupil) {
        PredictionType type = predictionTypeService.getByName(PredictionTypeEnum.MATH);
        if (type == null) {
            throw invalidResponse("PredictionType MATH row is missing in DB");
        }
        //mathPredictionRepository.deleteAllByPupilId(pupil.getId());
        List<MathPrediction> entities = r.professions().stream()
                .map(p -> MathPrediction.builder()
                        .pupil(pupil)
                        .predictionType(type)
                        .percentage(p.percentage())
                        .recommendation(p.recommendation())
                        .recommendationComplex(p.recommendationComplex())
                        .profession(p.profession())
                        .aizenNorm(p.aizenNorm())
                        .belbinNorm(p.belbinNorm())
                        .bennetNorm(p.bennetNorm())
                        .finalScore(p.finalScore())
                        .utility(p.utility())
                        .build())
                .toList();

        return mathPredictionRepository.saveAll(entities).stream()
                .map(e -> MathPredictionDTO.builder()
                        .id(e.getId())
                        .pupilId(e.getPupil().getId())
                        .predictionType(e.getPredictionType().getName())
                        .createdAt(e.getCreatedAt())
                        .percentage(e.getPercentage())
                        .recommendation(e.getRecommendation())
                        .recommendationComplex(e.getRecommendationComplex())
                        .aizenNorm(e.getAizenNorm())
                        .belbinNorm(e.getBelbinNorm())
                        .bennetNorm(e.getBennetNorm())
                        .finalScore(e.getFinalScore())
                        .utility(e.getUtility())
                        .profession(e.getProfession())
                        .build())
                .toList();
    }
    @Override
    public Optional<LocalDateTime> lastPredictionAt(Long pupilId) {
        return mathPredictionRepository
                .findTopByPupilIdOrderByCreatedAtDesc(pupilId)
                .map(MathPrediction::getCreatedAt);
    }

    private PredictionIntegrationException invalidResponse(String message) {
        log.warn(message);
        return new PredictionIntegrationException(
                "PREDICTION_INVALID_RESPONSE",
                HttpStatus.BAD_GATEWAY,
                "Prediction service returned an invalid response");
    }
}
