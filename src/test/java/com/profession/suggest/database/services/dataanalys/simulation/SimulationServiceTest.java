package com.profession.suggest.database.services.dataanalys.simulation;

import com.profession.suggest.database.entities.auth.Account;
import com.profession.suggest.database.entities.users.pupil.Pupil;
import com.profession.suggest.database.repositories.dataanalys.simulation.SimulationRepository;
import com.profession.suggest.database.services.profession.ProfessionService;
import com.profession.suggest.database.services.pupil.PupilService;
import com.profession.suggest.dto.dataanalys.simulation.SimulationDTO;
import com.profession.suggest.dto.dataanalys.simulation.SimulationMapper;
import com.profession.suggest.services.files.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SimulationServiceTest {
    @Mock SimulationRepository repository;
    @Mock SimulationTypeService simulationTypeService;
    @Mock ProfessionService professionService;
    @Mock ScenarioService scenarioService;
    @Mock SimulationDataSourceService simulationDataSourceService;
    @Mock PupilService pupilService;
    @Mock FileStorageService fileStorageService;
    @Mock SimulationMapper mapper;
    @Mock MultipartFile file;

    @InjectMocks SimulationService service;

    @Test
    void pupilCannotUploadSimulationForAnotherAccount() {
        Account requester = new Account();
        requester.setEmail("pupil-a@example.test");
        requester.setPupil(new Pupil());

        SimulationDTO dto = new SimulationDTO();
        dto.setEmail("pupil-b@example.test");

        assertThrows(SecurityException.class,
                () -> service.createSimulation(dto, file, requester));
        verifyNoInteractions(repository, fileStorageService);
    }
}
