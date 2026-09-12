package com.profession.suggest.dto.dataanalys.prediction.math;

import com.profession.suggest.database.entities.dataanalys.prediction.math.MathPrediction;
import org.springframework.stereotype.Component;

@Component
public class MathPredictionMapper {

    public MathPredictionDTO toDTO(MathPrediction entity) {
        if (entity == null) {
            return null;
        }
        return MathPredictionDTO.builder()
                .id(entity.getId())
                .pupilId(entity.getPupil() != null ? entity.getPupil().getId() : null)
                .predictionType(entity.getPredictionType() != null
                        ? entity.getPredictionType().getName()
                        : null)
                .createdAt(entity.getCreatedAt())
                .percentage(entity.getPercentage())
                .recommendation(entity.getRecommendation())
                .aizenNorm(entity.getAizenNorm())
                .belbinNorm(entity.getBelbinNorm())
                .bennetNorm(entity.getBennetNorm())
                .finalScore(entity.getFinalScore())
                .utility(entity.getUtility())
                .build();
    }
}
