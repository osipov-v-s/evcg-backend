package com.profession.suggest.controllers.dataanalys.simulation;

import com.profession.suggest.configuration.security.annotation.HasRole;
import com.profession.suggest.database.entities.auth.Account;
import com.profession.suggest.database.entities.auth.role.RoleEnum;
import com.profession.suggest.database.entities.dataanalys.simulation.Simulation;
import com.profession.suggest.database.services.auth.AccountService;
import com.profession.suggest.database.services.dataanalys.simulation.ScenarioService;
import com.profession.suggest.database.services.dataanalys.simulation.SimulationDataSourceService;
import com.profession.suggest.database.services.dataanalys.simulation.SimulationService;
import com.profession.suggest.database.services.dataanalys.simulation.SimulationTypeService;
import com.profession.suggest.dto.dataanalys.simulation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.Pageable;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/simulations")
public class SimulationController {
    private final SimulationService simulationService;
    private final SimulationTypeService simulationTypeService;
    private final ScenarioService scenarioService;
    private final SimulationDataSourceService simulationDataSourceService;
    private final SimulationMapper simulationMapper;
    private final AccountService accountService;

    public SimulationController(SimulationService simulationService, SimulationTypeService simulationTypeService, ScenarioService scenarioService, SimulationDataSourceService simulationDataSourceService, SimulationMapper simulationMapper, AccountService accountService) {
        this.simulationService = simulationService;
        this.simulationTypeService = simulationTypeService;
        this.scenarioService = scenarioService;
        this.simulationDataSourceService = simulationDataSourceService;
        this.simulationMapper = simulationMapper;
        this.accountService = accountService;
    }
    @HasRole({RoleEnum.PUPIL, RoleEnum.ADMIN})
    @PostMapping("/create")
    public ResponseEntity<SimulationDTO> createSimulation(@RequestPart("metadata") SimulationDTO simulationDTO,
                                                           @RequestPart("file") MultipartFile file,
                                                           @RequestAttribute("accountId") Long accountId) throws Exception {
        Account requester = accountService.getAccountById(accountId);
        return ResponseEntity.ok(simulationMapper
                .toDTO(simulationService
                        .createSimulation(simulationDTO, file, requester), simulationDTO.getEmail()));
    }
    @HasRole(RoleEnum.ADMIN)
    @GetMapping
    public ResponseEntity<Page<SimulationResponseDTO>> getSimulations(@RequestParam(required = false) String email,
                                                                      @RequestParam(required = false) LocalDateTime startSimulation,
                                                                      @RequestParam(required = false) LocalDateTime endSimulation,
                                                                      @RequestParam(required = false) String simulationType,
                                                                      @RequestParam(required = false) String profession,
                                                                      @RequestParam(required = false) String scenario,
                                                                      @RequestParam(required = false) String simulationDataSource,
                                                                      @RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = "10") int size,
                                                                      @RequestParam(defaultValue = "createdAt") String sortBy) throws UnsupportedEncodingException {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString("desc"), sortBy));
        Page<Simulation> simulations = simulationService.findByFilters(email,
                startSimulation, endSimulation,
                simulationType, profession , scenario,
                simulationDataSource, pageable);
        return ResponseEntity.ok(
                simulations.map(s -> simulationMapper.toResponseDTO(
                        s, s.getPupil().getAccount() != null ? s.getPupil().getAccount().getEmail() : null)));
    }
    @HasRole(RoleEnum.ADMIN)
    @PatchMapping("/{simulationId}/description")
    public ResponseEntity<SimulationDTO> updateDescription(@PathVariable("simulationId") Long simulationId,
                                               @RequestBody String description) {
        ;
        return ResponseEntity.ok(simulationService.updateDescriptionById(simulationId, description));
    }
    @HasRole({RoleEnum.ADMIN, RoleEnum.PUPIL, RoleEnum.SPECIALIST, RoleEnum.CURATOR})
    @GetMapping("/types")
    public ResponseEntity<List<SimulationTypeDTO>> getSimulationTypes() {
        return ResponseEntity.ok(simulationTypeService.getSimulationTypes());
    }
    @HasRole(RoleEnum.ADMIN)
    @PostMapping("/types")
    public ResponseEntity<SimulationTypeDTO> createSimulationType(@RequestBody SimulationTypeDTO simulationTypeDTO) {
        return ResponseEntity.ok(simulationTypeService.createSimulation(simulationTypeDTO));
    }
    @HasRole({RoleEnum.ADMIN, RoleEnum.PUPIL, RoleEnum.SPECIALIST, RoleEnum.CURATOR})
    @GetMapping("/scenarios")
    public ResponseEntity<List<ScenarioDTO>> getScenario() {
        return ResponseEntity.ok(scenarioService.getAllScenarios());
    }
    @HasRole({RoleEnum.ADMIN, RoleEnum.PUPIL, RoleEnum.SPECIALIST, RoleEnum.CURATOR})
    @GetMapping("/simulation-data-sources")
    public ResponseEntity<List<SimulationDataSourceDTO>> getSimulationDataSources() {
        return ResponseEntity.ok(simulationDataSourceService.getSimulationDataSources());
    }
}
