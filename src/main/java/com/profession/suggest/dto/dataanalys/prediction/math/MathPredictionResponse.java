package com.profession.suggest.dto.dataanalys.prediction.math;

import java.util.List;

public record MathPredictionResponse(
        Long pupilId,
        List<PredictedProfession> professions
) {

    public record PredictedProfession(
        Float percentage,
        String recommendation,
        String recommendationComplex,
        String profession,
        Float aizenNorm,
        Float belbinNorm,
        Float bennetNorm,
        Float finalScore,
        Float utility
    ){}
}