package com.profession.suggest.database.services.dataanalys.prediction;

import com.profession.suggest.database.entities.dataanalys.prediction.Prediction;
import com.profession.suggest.database.entities.users.pupil.Pupil;
import com.profession.suggest.database.repositories.dataanalys.prediction.PredictionRepository;
import com.profession.suggest.database.services.pupil.PupilService;
import com.profession.suggest.dto.dataanalys.prediction.PredictionDTO;
import com.profession.suggest.dto.dataanalys.prediction.PredictionMapper;
import com.profession.suggest.dto.dataanalys.prediction.PredictionResponse;
import com.profession.suggest.exceptions.PredictionIntegrationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClusterPredictionService {

    private final PredictionRepository repository;
    private final PredictionMapper mapper;
    private final PupilService pupilService;

    public List<PredictionDTO> getByPupilId(Long pupilId) {
        return repository.findByPupilId(pupilId).stream()
                .map(mapper::toDTO)
                .toList();
    }

    public PredictionResponse getLatestByAccountId(Long accountId) {
        Pupil pupil = pupilService.getPupilByAccountId(accountId);
        Prediction p = repository.findTopByPupilIdOrderByCreatedAtDesc(pupil.getId())
                .orElseThrow(() -> notFound("No cluster prediction found"));
        return toResponse(p);
    }

    public PredictionResponse toResponse(Prediction p) {
        return PredictionResponse.builder()
                .pupilId(p.getPupil().getId())
                .cluster(p.getCluster())
                .predictedProfession(p.getPredictedProfession().getName())
                .nearestSpecialistId(p.getNearestSpecialist().getId())
                .distance(p.getDistance())
                .confidenceCategory(p.getConfidenceCategory())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private PredictionIntegrationException notFound(String msg) {
        return new PredictionIntegrationException(
                "PREDICTION_NOT_FOUND", HttpStatus.NOT_FOUND, msg);
    }
}