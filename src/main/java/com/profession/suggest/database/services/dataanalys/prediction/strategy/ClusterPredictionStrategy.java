package com.profession.suggest.database.services.dataanalys.prediction.strategy;

import com.profession.suggest.database.entities.auth.Account;
import com.profession.suggest.database.entities.dataanalys.prediction.Prediction;
import com.profession.suggest.database.entities.dataanalys.prediction.PredictionType;
import com.profession.suggest.database.entities.dataanalys.prediction.PredictionTypeEnum;
import com.profession.suggest.database.entities.professions.Profession;
import com.profession.suggest.database.entities.users.pupil.Pupil;
import com.profession.suggest.database.entities.users.specialist.Specialist;
import com.profession.suggest.database.repositories.dataanalys.prediction.PredictionRepository;
import com.profession.suggest.database.services.dataanalys.prediction.PredictionStrategy;
import com.profession.suggest.database.services.dataanalys.prediction.PredictionTypeService;
import com.profession.suggest.database.services.dataanalys.psychtests.PsychTestService;
import com.profession.suggest.database.services.profession.ProfessionService;
import com.profession.suggest.database.services.specialist.SpecialistService;
import com.profession.suggest.dto.dataanalys.prediction.PredictionRequest;
import com.profession.suggest.dto.dataanalys.prediction.PredictionResponse;
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
public class ClusterPredictionStrategy
        implements PredictionStrategy<PredictionResponse, Prediction> {

    private final RestTemplate restTemplate;
    private final PsychTestService psychTestService;
    private final ProfessionService professionService;
    private final SpecialistService specialistService;
    private final PredictionTypeService predictionTypeService;
    private final PredictionRepository predictionRepository;

    @Override
    public PredictionTypeEnum type() {
        return PredictionTypeEnum.CLUSTER;
    }

    @Override
    public PredictionRequest buildRequest(Account account, Pupil pupil) {
        return new PredictionRequest(
                pupil.getId(),
                pupil.getBirthday() != null
                        ? Period.between(pupil.getBirthday(), LocalDate.now()).getYears()
                        : 14,
                psychTestService.getAccountRecentTests(account));
    }

    @Override
    public PredictionResponse call(PredictionRequest request, String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PredictionRequest> entity = new HttpEntity<>(request, headers);

        log.info("CLUSTER call pupilId={} url={}", request.getPupilId(), url);

        ResponseEntity<PredictionResponse> response =
                restTemplate.exchange(url, HttpMethod.POST, entity, PredictionResponse.class);

        if (response.getBody() == null) {
            throw invalidResponse("Cluster prediction service returned an empty response");
        }
        return response.getBody();
    }

    @Override
    public void validate(PredictionResponse r, Long expectedPupilId) {
        if (r == null
                || !expectedPupilId.equals(r.getPupilId())     // value compare, not reference
                || r.getCluster() < 0
                || r.getNearestSpecialistId() <= 0
                || !Double.isFinite(r.getDistance())
                || r.getDistance() < 0
                || r.getPredictedProfession() == null
                || r.getPredictedProfession().isBlank()
                || r.getConfidenceCategory() == null
                || r.getConfidenceCategory().isBlank()) {
            throw invalidResponse("Cluster prediction service returned an invalid response");
        }
    }

    @Override
    public Prediction save(PredictionResponse r, Pupil pupil) {
        Profession profession = professionService.getProfessionByName(r.getPredictedProfession());
        Specialist specialist = specialistService.getSpecialistById(r.getNearestSpecialistId());
        PredictionType type = predictionTypeService.getByName(PredictionTypeEnum.CLUSTER);

        if (profession == null || specialist == null || type == null) {
            throw invalidResponse("Cluster response references missing domain data");
        }

        return predictionRepository.save(Prediction.builder()
                .pupil(pupil)
                .predictedProfession(profession)
                .nearestSpecialist(specialist)
                .predictionType(type)
                .cluster(r.getCluster())
                .distance(r.getDistance())
                .confidenceCategory(r.getConfidenceCategory())
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
