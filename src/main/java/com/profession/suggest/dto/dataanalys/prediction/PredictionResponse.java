package com.profession.suggest.dto.dataanalys.prediction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionResponse {
    private long pupilId;
    private int cluster;
    private String predictedProfession;
    private long nearestSpecialistId;
    private double distance;
    private String confidenceCategory;
    private LocalDateTime createdAt;
}