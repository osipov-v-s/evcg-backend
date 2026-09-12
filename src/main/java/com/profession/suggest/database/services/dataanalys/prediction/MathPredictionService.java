package com.profession.suggest.database.services.dataanalys.prediction;

import com.profession.suggest.database.entities.dataanalys.prediction.math.MathPrediction;
import com.profession.suggest.database.entities.users.pupil.Pupil;
import com.profession.suggest.database.repositories.dataanalys.prediction.MathPredictionRepository;
import com.profession.suggest.database.services.pupil.PupilService;
import com.profession.suggest.dto.dataanalys.prediction.math.MathPredictionDTO;
import com.profession.suggest.dto.dataanalys.prediction.math.MathPredictionMapper;
import com.profession.suggest.exceptions.PredictionIntegrationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MathPredictionService {

    private final MathPredictionRepository repository;
    private final MathPredictionMapper mapper;
    private final PupilService pupilService;

    public List<MathPredictionDTO> getByPupilId(Long pupilId) {
        return repository.findByPupilId(pupilId).stream()
                .map(mapper::toDTO)
                .toList();
    }
    public MathPredictionDTO toDTO(MathPrediction mathPrediction) {
        return mapper.toDTO(mathPrediction);
    }

    public MathPredictionDTO getLatestByAccountId(Long accountId) {
        Pupil pupil = pupilService.getPupilByAccountId(accountId);
        MathPrediction p = repository.findTopByPupilIdOrderByCreatedAtDesc(pupil.getId())
                .orElseThrow(() -> notFound("No math prediction found"));
        return mapper.toDTO(p);
    }

    private PredictionIntegrationException notFound(String msg) {
        return new PredictionIntegrationException(
                "PREDICTION_NOT_FOUND", HttpStatus.NOT_FOUND, msg);
    }
}