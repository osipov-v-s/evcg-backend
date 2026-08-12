package com.profession.suggest.exceptions;

import com.profession.suggest.dto.dataanalys.prediction.PredictionErrorDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class PredictionExceptionHandler {
    @ExceptionHandler(PredictionIntegrationException.class)
    public ResponseEntity<PredictionErrorDTO> handlePredictionIntegration(
            PredictionIntegrationException exception) {
        return ResponseEntity.status(exception.getStatus()).body(new PredictionErrorDTO(
                exception.getCode(),
                exception.getSafeMessage(),
                LocalDateTime.now()));
    }
}
