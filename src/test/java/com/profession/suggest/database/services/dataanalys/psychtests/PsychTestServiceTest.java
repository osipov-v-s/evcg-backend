package com.profession.suggest.database.services.dataanalys.psychtests;

import com.profession.suggest.database.entities.auth.Account;
import com.profession.suggest.database.entities.dataanalys.psychtests.PsychTest;
import com.profession.suggest.database.entities.dataanalys.psychtests.PsychTestType;
import com.profession.suggest.database.entities.users.pupil.Pupil;
import com.profession.suggest.database.entities.education.School;
import com.profession.suggest.database.repositories.dataanalys.psychtests.PsychTestRepository;
import com.profession.suggest.database.services.pupil.PupilService;
import com.profession.suggest.dto.dataanalys.psychtests.PsychTestDTO;
import com.profession.suggest.dto.dataanalys.psychtests.PsychTestMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class PsychTestServiceTest {
    @Mock PsychTestRepository repository;
    @Mock PsychParamService psychParamService;
    @Mock PupilService pupilService;
    @Mock PsychTestTypeService psychTestTypeService;
    @Mock PsychTestMapper mapper;

    @InjectMocks PsychTestService service;

    @Test
    void savedResultIsLinkedToAuthenticatedPupil() {
        Pupil pupil = new Pupil();
        Account account = new Account();
        account.setPupil(pupil);
        PsychTestDTO request = new PsychTestDTO();
        request.setPsychParams(java.util.List.of());
        request.setTestTypeName("temperament");
        PsychTest test = new PsychTest();
        test.setPsychParams(new HashSet<>());
        PsychTestType type = new PsychTestType();
        PsychTestDTO response = new PsychTestDTO();

        when(psychTestTypeService.getPsychTestTypeByName("temperament")).thenReturn(type);
        when(mapper.fromDTO(request)).thenReturn(test);
        when(repository.save(any(PsychTest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toDTO(test)).thenReturn(response);

        assertSame(response, service.createPsychTest(request, account));
        assertSame(pupil, test.getPupil());
        assertNull(test.getSpecialist());
        assertSame(type, test.getPsychTestType());
    }

    @Test
    void curatorExportUsesSchoolScopedRepositoryQuery() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 31, 23, 59);
        School school = new School();
        school.setId(10L);
        school.setName("School A");
        Account account = new Account();
        account.setId(100L);
        account.setEmail("pupil@example.test");
        account.setRoles(new HashSet<>());
        Pupil pupil = new Pupil();
        pupil.setName("Ivan");
        pupil.setSurname("Ivanov");
        pupil.setPatronymic("Ivanovich");
        pupil.setSchool("School A");
        pupil.setEducationalOrganization(school);
        pupil.setAccount(account);
        PsychTest test = new PsychTest();
        test.setPupil(pupil);
        PsychTestDTO dto = new PsychTestDTO();

        when(repository.findByPupilSchoolAndDateRange(10L, start, end))
                .thenReturn(java.util.List.of(test));
        when(mapper.toDTO(test)).thenReturn(dto);

        var result = service.getCompletedPupilTestsBySchoolAndDateRange(10L, start, end);

        assertEquals(1, result.size());
        assertEquals("School A", result.get(0).getSchool());
        assertSame(dto, result.get(0).getPsychTests().get(0));
        verify(repository).findByPupilSchoolAndDateRange(10L, start, end);
    }
}
