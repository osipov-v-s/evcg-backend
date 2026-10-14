package com.profession.suggest.database.services.dataanalys.simulation;

import com.profession.suggest.database.entities.auth.Account;
import com.profession.suggest.database.entities.auth.role.Role;
import com.profession.suggest.database.entities.auth.role.RoleEnum;
import com.profession.suggest.database.entities.dataanalys.simulation.Scenario;
import com.profession.suggest.database.entities.dataanalys.simulation.Simulation;
import com.profession.suggest.database.entities.dataanalys.simulation.SimulationDataSource;
import com.profession.suggest.database.entities.dataanalys.simulation.SimulationType;
import com.profession.suggest.database.entities.files.FileKind;
import com.profession.suggest.database.entities.files.StoredFile;
import com.profession.suggest.database.entities.professions.Profession;
import com.profession.suggest.database.entities.users.pupil.Pupil;
import com.profession.suggest.database.repositories.dataanalys.simulation.SimulationRepository;
import com.profession.suggest.database.services.auth.AccountService;
import com.profession.suggest.database.services.file.StoredFileService;
import com.profession.suggest.database.services.profession.ProfessionService;
import com.profession.suggest.database.services.pupil.PupilService;
import com.profession.suggest.dto.dataanalys.simulation.SimulationDTO;
import com.profession.suggest.dto.dataanalys.simulation.SimulationJsonCreateRequest;
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

import javax.security.auth.login.AccountNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;

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
    private final AccountService accountService;
    private final StoredFileService storedFileService;
    private final SimulationMapper mapper;
    /* TODO
    * simulation can be created even if account with email doesn't exists
    * (register account with default password and default pupil and continue)
    * */

    @Transactional
    public Simulation createFromFileId(SimulationJsonCreateRequest req, Long accountId) throws AccountNotFoundException {
        Account requester = accountService.getAccountById(accountId);
        if (req.getFileId() == null)
            throw new IllegalArgumentException("field is required");
        StoredFile stored = storedFileService.getStoredFile(req.getFileId());
        if (stored.getKind() != FileKind.SIMULATION)
            throw new IllegalArgumentException("field must reference a SMULATION file");
        boolean isAdmin = requester.getRoles().stream()
                .map(Role::getName)
                .anyMatch(r -> r == RoleEnum.ADMIN);
        if (!isAdmin && !Objects.equals(stored.getOwnerAccountId(), requester.getId()))
            throw new SecurityException("fileId does not belong to requester");
        SimulationDTO dto = new SimulationDTO();
        dto.setEmail(req.getEmail());
        dto.setSimulationType(req.getSimulationType());
        dto.setScenario(req.getScenario());
        dto.setSimulationDataSource(req.getSimulationDataSource());
        dto.setProfession(req.getProfession());
        dto.setStartSimulation(req.getStartSimulation());
        dto.setEndSimulation(req.getEndSimulation());
        dto.setDescription(req.getDescription());
        try {
            return persistSimulation(dto, stored, requester);
        } catch (SecurityException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create simulation", e);
        }
    }
    private Simulation persistSimulation(SimulationDTO dto, StoredFile stored, Account requester) throws Exception {
        Simulation simulation = new Simulation();
        simulation.setSimulationType(simulationTypeService.getByName(dto.getSimulationType()));
        simulation.setScenario(scenarioService.getScenarioByName(dto.getScenario()));
        simulation.setSimulationDataSource(simulationDataSourceService.getByName(dto.getSimulationDataSource()));
        simulation.setProfession(professionService.getProfessionByName(dto.getProfession()));
        Pupil pupil = resolvePupil(dto, requester);
        simulation.setPupil(pupil);
        simulation.setStartSimulation(dto.getStartSimulation());
        simulation.setEndSimulation(dto.getEndSimulation());
        simulation.setDescription(dto.getDescription());
        simulation.setFile(stored);

        Long pupilAccountId = pupil.getAccount() != null ? pupil.getAccount().getId() : null;
        if (pupilAccountId != null && Objects.equals(stored.getOwnerAccountId(), pupilAccountId)){
            stored.setOwnerAccountId(pupilAccountId);
            storedFileService.save(stored);
        }
        return repository.save(simulation);
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
