package com.profession.suggest.dto.dataanalys.prediction.math;

import com.profession.suggest.database.entities.dataanalys.prediction.PredictionTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MathPredictionDTO {

    // shared / identity fields
    private Long id;
    private Long pupilId;
    private PredictionTypeEnum predictionType;
    private LocalDateTime createdAt;

    // math-specific fields
    private Float percentage;
    private String recommendation;
    private Float aizenNorm;
    private Float belbinNorm;
    private Float bennetNorm;
    private Float finalScore;
    private Float utility;
}
