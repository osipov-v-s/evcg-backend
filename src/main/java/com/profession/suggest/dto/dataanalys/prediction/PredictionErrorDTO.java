package com.profession.suggest.dto.dataanalys.prediction;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class PredictionErrorDTO {
    private String code;
    private String message;
    private LocalDateTime timestamp;
}
