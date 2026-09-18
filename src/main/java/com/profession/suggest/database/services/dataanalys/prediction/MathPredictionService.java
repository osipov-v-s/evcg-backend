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

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

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

    public List<MathPredictionDTO> getLatestByAccountId(Long accountId) {
        Pupil pupil = pupilService.getPupilByAccountId(accountId);
        List<MathPrediction> mathPredictions = repository.findByPupilId(pupil.getId());
        return mathPredictions.stream()
                .sorted(Comparator.comparing(MathPrediction::getCreatedAt).reversed())
                .collect(Collectors.toMap(
                        MathPrediction::getProfession,
                        mapper::toDTO,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                )).values().stream().toList();
    }

    private PredictionIntegrationException notFound(String msg) {
        return new PredictionIntegrationException(
                "PREDICTION_NOT_FOUND", HttpStatus.NOT_FOUND, msg);
    }
}