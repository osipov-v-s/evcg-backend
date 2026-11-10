package com.profession.suggest.dto.dataanalys.comparison;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionData {
    private String clientSessionId;
    private Long collectionId;
    private Long trackingFileId;
    private String appVersion;
    private String dataSchemaVersion;
    private String dataSourceMode;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Double totalDurationSeconds;
    private Boolean isCompleted;
    private Boolean eyeTrackingAvailable;
    private Boolean faceTrackingAvailable;
    private String uploadStatus;
    private Integer recordCount;
}