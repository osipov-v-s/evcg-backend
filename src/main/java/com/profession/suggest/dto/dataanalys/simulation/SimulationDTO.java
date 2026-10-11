package com.profession.suggest.dto.dataanalys.simulation;

import com.profession.suggest.dto.file.StoredFileDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimulationDTO {
    private Long id;
    private String filePath;
    private StoredFileDTO file;
    private LocalDateTime startSimulation;
    private LocalDateTime endSimulation;
    private LocalDateTime createdAt;
    private String email;
    private String simulationType;
    private String profession;
    private String scenario;
    private String simulationDataSource;
    private String description;

}
