package com.profession.suggest.dto.dataanalys.prediction.math;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MathPredictionResponse {
    private Long pupilId;
    private Float percentage;
    private String recommendation;
    private Float aizenNorm;
    private Float belbinNorm;
    private Float bennetNorm;
    private Float finalScore;
    private Float utility;

}
