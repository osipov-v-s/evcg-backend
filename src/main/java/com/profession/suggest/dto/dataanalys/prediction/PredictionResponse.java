package com.profession.suggest.dto.dataanalys.prediction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PredictionResponse {
    private long pupilId;
    private int cluster;
    private String predictedProfession;
    private long nearestSpecialistId;
    private double distance;
    private String confidenceCategory;
}