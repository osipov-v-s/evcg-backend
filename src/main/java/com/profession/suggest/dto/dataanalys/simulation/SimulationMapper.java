package com.profession.suggest.dto.dataanalys.simulation;

import com.profession.suggest.database.entities.dataanalys.simulation.Scenario;
import com.profession.suggest.database.entities.dataanalys.simulation.Simulation;
import com.profession.suggest.database.entities.dataanalys.simulation.SimulationDataSource;
import com.profession.suggest.database.entities.dataanalys.simulation.SimulationType;
import com.profession.suggest.database.entities.professions.Profession;
import com.profession.suggest.database.entities.users.pupil.Pupil;
import com.profession.suggest.services.files.FileUrlService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@AllArgsConstructor
public class SimulationMapper {
    private final FileUrlService fileUrlService;
    public Simulation fromDTO(SimulationDTO dto,
                              SimulationType simulationType,
                              Profession profession,
                              Pupil pupil,
                              String filepath) {
        Simulation simulation = new Simulation();
        simulation.setStartSimulation(dto.getStartSimulation());
        simulation.setEndSimulation(dto.getEndSimulation());
        simulation.setSimulationType(simulationType);
        simulation.setProfession(profession);
        simulation.setPupil(pupil);
        simulation.setFilePath(filepath);
        return simulation;
    }
    public SimulationDTO toDTO(Simulation simulation, String email) {
        SimulationDTO dto = new SimulationDTO();
        dto.setId(simulation.getId());
        dto.setEmail(email);
        dto.setStartSimulation(simulation.getStartSimulation());
        dto.setEndSimulation(simulation.getEndSimulation());

        dto.setProfession(Optional.ofNullable(simulation.getProfession())
                .map(Profession::getName).orElse(null));
        dto.setSimulationType(Optional.ofNullable(simulation.getSimulationType())
                .map(SimulationType::getName)
                .orElse(null));
        dto.setScenario(Optional.ofNullable(simulation.getScenario())
                .map(Scenario::getName)
                .orElse(null));
        dto.setSimulationDataSource(Optional.ofNullable(simulation.getSimulationDataSource())
                .map(SimulationDataSource::getName)
                .orElse(null));
        dto.setDescription(simulation.getDescription());

        dto.setFile(fileUrlService.toDto(simulation.getFile()));
        dto.setFilePath(simulation.getFilePath());
        dto.setCreatedAt(simulation.getCreatedAt());
        return dto;
    }
    public SimulationResponseDTO toResponseDTO(Simulation simulation, String email) {
        SimulationDTO dto = toDTO(simulation, email);
        SimulationResponseDTO simulationResponseDTO = new SimulationResponseDTO();

        simulationResponseDTO.setSimulation(dto);
        simulationResponseDTO.setId(simulation.getId());
        return simulationResponseDTO;
    }
}
