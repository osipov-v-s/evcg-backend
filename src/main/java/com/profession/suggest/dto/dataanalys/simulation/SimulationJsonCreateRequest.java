package com.profession.suggest.dto.dataanalys.simulation;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SimulationJsonCreateRequest {
    private Long fileId;
    private String email;
    private String simulationType;
    private String scenario;
    private String simulationDataSource;
    private String profession;
    private LocalDateTime startSimulation;
    private LocalDateTime endSimulation;
    private String description;
}
