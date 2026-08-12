package com.profession.suggest.database.services.dataanalys.simulation;

import com.profession.suggest.database.entities.auth.Account;
import com.profession.suggest.database.entities.auth.role.RoleEnum;
import com.profession.suggest.database.entities.dataanalys.simulation.Scenario;
import com.profession.suggest.database.entities.dataanalys.simulation.Simulation;
import com.profession.suggest.database.entities.dataanalys.simulation.SimulationDataSource;
import com.profession.suggest.database.entities.dataanalys.simulation.SimulationType;
import com.profession.suggest.database.entities.professions.Profession;
import com.profession.suggest.database.entities.users.pupil.Pupil;
import com.profession.suggest.database.repositories.dataanalys.simulation.SimulationRepository;
import com.profession.suggest.database.services.profession.ProfessionService;
import com.profession.suggest.database.services.pupil.PupilService;
import com.profession.suggest.dto.dataanalys.simulation.SimulationDTO;
import com.profession.suggest.dto.dataanalys.simulation.SimulationMapper;
import com.profession.suggest.services.files.FileStorageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.Pageable;
import java.io.IOException;
import java.time.LocalDateTime;

@Service
@AllArgsConstructor
@Slf4j
public class SimulationService {
    private final SimulationRepository repository;
    private final SimulationTypeService simulationTypeService;
    private final ProfessionService professionService;
    private final ScenarioService scenarioService;
    private final SimulationDataSourceService simulationDataSourceService;
    private final PupilService pupilService;
    private final FileStorageService fileStorageService;
    private final SimulationMapper mapper;
    /* TODO
    * simulation can be created even if account with email doesn't exists
    * (register account with default password and default pupil and continue)
    * */
    @Transactional
    public Simulation createSimulation(SimulationDTO simulationDTO, MultipartFile file, Account requester) {
        try {

            Simulation simulation = new Simulation();
            SimulationType simulationType = simulationTypeService.getByName(simulationDTO.getSimulationType());
            Scenario scenario = scenarioService.getScenarioByName(simulationDTO.getScenario());
            SimulationDataSource simulationDataSource = simulationDataSourceService.getByName(simulationDTO.getSimulationDataSource());
            Profession profession = professionService.getProfessionByName(simulationDTO.getProfession());
            Pupil pupil = resolvePupil(simulationDTO, requester);
            //if there is no pupil found by account email,
            // that means need to create Account with email and Pupil
            simulation.setSimulationType(simulationType);
            simulation.setProfession(profession);
            simulation.setScenario(scenario);
            simulation.setSimulationDataSource(simulationDataSource);
            simulation.setStartSimulation(simulationDTO.getStartSimulation());
            simulation.setEndSimulation(simulationDTO.getEndSimulation());
            simulation.setPupil(pupil);
            simulation.setFilePath(fileStorageService.saveFile(file, "simulations", true));

            return repository.save(simulation);
        } catch (IOException e) {
            log.error("Error while saving simulation file for email {}", simulationDTO.getEmail() ,e);
            throw new RuntimeException("Failed to save a file", e);
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error occurred while creating simulation for email: {}", simulationDTO.getEmail() ,e);
            throw new RuntimeException("Failed to create simulation for email",  e);
        }
    }

    private Pupil resolvePupil(SimulationDTO simulationDTO, Account requester) throws Exception {
        if (requester.getPupil() != null) {
            String requesterEmail = requester.getEmail();
            if (simulationDTO.getEmail() != null
                    && !simulationDTO.getEmail().isBlank()
                    && !requesterEmail.equalsIgnoreCase(simulationDTO.getEmail()))
                throw new SecurityException("Simulation cannot be assigned to another pupil");
            simulationDTO.setEmail(requesterEmail);
            return requester.getPupil();
        }

        boolean isAdmin = requester.getRoles().stream()
                .map(role -> role.getName())
                .anyMatch(role -> role == RoleEnum.ADMIN);
        if (!isAdmin)
            throw new SecurityException("Only a pupil or administrator can upload simulation data");
        if (simulationDTO.getEmail() == null || simulationDTO.getEmail().isBlank())
            throw new IllegalArgumentException("email is required for administrator upload");
        return pupilService.getPupilByAccountEmail(simulationDTO.getEmail())
                .orElse(pupilService.createWithDefaults(simulationDTO.getEmail()));
    }
    public Page<Simulation> findByFilters(String email,
                                          LocalDateTime startSimulation, LocalDateTime endSimulation,
                                          String simulationType, String profession, String scenario,
                                          String simulationDataSource, Pageable pageable) {
        Specification<Simulation> spec = (root, query, cb) -> cb.conjunction();
        if (email != null && !email.isEmpty())
            spec = spec.and(((root, query, cb) ->
                    cb.equal(root.get("pupil").get("account").get("email"), email)));
        if (startSimulation != null)
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("startSimulation"), startSimulation));
        if (endSimulation != null)
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("endSimulation"), endSimulation));
        if (simulationType != null && !simulationType.isEmpty())
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("simulationType").get("name"), simulationType));
        if (profession != null && !profession.isEmpty())
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("profession").get("name"), profession));
        if (scenario != null && !scenario.isEmpty())
            spec = spec.and((((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("scenario").get("name"), scenario))));
        if (simulationDataSource != null && !simulationDataSource.isEmpty())
            spec = spec.and(((root, query, criteriaBuilder) -> criteriaBuilder
                    .equal(root.get("simulationDataSource").get("name"), simulationDataSource)));
        return repository.findAll(spec, pageable);

    }
    public SimulationDTO updateDescriptionById(Long id, String description) {
        Simulation simulation = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cant find a simulation with id = " + id ));
        simulation.setDescription(description);
        return mapper.toDTO(repository.save(simulation), simulation.getPupil().getAccount().getEmail());
    }
}
