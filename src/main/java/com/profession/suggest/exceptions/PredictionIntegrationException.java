package com.profession.suggest.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PredictionIntegrationException extends RuntimeException {
    private final String code;
    private final HttpStatus status;
    private final String safeMessage;

    public PredictionIntegrationException(String code,
                                          HttpStatus status,
                                          String safeMessage,
                                          Throwable cause) {
        super(safeMessage, cause);
        this.code = code;
        this.status = status;
        this.safeMessage = safeMessage;
    }

    public PredictionIntegrationException(String code,
                                          HttpStatus status,
                                          String safeMessage) {
        this(code, status, safeMessage, null);
    }
}
