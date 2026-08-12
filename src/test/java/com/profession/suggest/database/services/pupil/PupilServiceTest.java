package com.profession.suggest.database.services.pupil;

import com.profession.suggest.database.entities.auth.role.RoleEnum;
import com.profession.suggest.database.entities.auth.Account;
import com.profession.suggest.database.entities.education.School;
import com.profession.suggest.database.entities.users.pupil.Pupil;
import com.profession.suggest.database.repositories.pupil.PupilRepository;
import com.profession.suggest.database.services.auth.AccountService;
import com.profession.suggest.database.services.auth.role.RoleService;
import com.profession.suggest.database.services.education.CuratorService;
import com.profession.suggest.database.services.education.SchoolService;
import com.profession.suggest.database.services.gender.GenderService;
import com.profession.suggest.dto.auth.AccountMapper;
import com.profession.suggest.dto.pupil.PupilMapper;
import com.profession.suggest.dto.pupil.PupilResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class PupilServiceTest {
    @Mock PupilRepository repository;
    @Mock AccountService accountService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock GenderService genderService;
    @Mock RoleService roleService;
    @Mock PupilMapper pupilMapper;
    @Mock AccountMapper accountMapper;
    @Mock SchoolService schoolService;
    @Mock CuratorService curatorService;

    @InjectMocks PupilService service;

    @Test
    void curatorCanReadOnlyPupilsFromOwnSchool() throws Exception {
        School schoolA = school(10L, "School A");
        School schoolB = school(20L, "School B");
        Pupil pupilA = pupil(1L, schoolA);
        Pupil pupilB = pupil(2L, schoolB);
        PupilResponseDTO responseA = new PupilResponseDTO();

        when(accountService.getActiveRoleNamesByAccount(100L)).thenReturn(Set.of(RoleEnum.CURATOR));
        when(curatorService.getSchoolIdByAccountId(100L)).thenReturn(10L);
        when(repository.findById(1L)).thenReturn(Optional.of(pupilA));
        when(repository.findById(2L)).thenReturn(Optional.of(pupilB));
        when(pupilMapper.toResponseDTO(pupilA)).thenReturn(responseA);

        assertSame(responseA, service.getPupilDataForRequester(100L, 1L));
        assertThrows(AccessDeniedException.class,
                () -> service.getPupilDataForRequester(100L, 2L));
    }

    @Test
    void curatorCanResetPasswordOnlyForPupilFromOwnSchool() throws Exception {
        School schoolA = school(10L, "School A");
        School schoolB = school(20L, "School B");
        Pupil pupilA = pupil(1L, schoolA);
        Pupil pupilB = pupil(2L, schoolB);
        Account accountA = new Account();
        accountA.setId(11L);
        pupilA.setAccount(accountA);
        pupilB.setAccount(new Account());

        ReflectionTestUtils.setField(service, "defaultPassword", "123123");
        when(accountService.getActiveRoleNamesByAccount(100L)).thenReturn(Set.of(RoleEnum.CURATOR));
        when(curatorService.getSchoolIdByAccountId(100L)).thenReturn(10L);
        when(repository.findById(1L)).thenReturn(Optional.of(pupilA));
        when(repository.findById(2L)).thenReturn(Optional.of(pupilB));
        when(passwordEncoder.encode("123123")).thenReturn("hashed-password");

        service.resetPasswordForCurator(100L, 1L);

        assertEquals("hashed-password", accountA.getPassword());
        assertEquals(Boolean.TRUE, accountA.getFirstLogin());
        verify(accountService).createAccount(accountA);

        assertThrows(AccessDeniedException.class,
                () -> service.resetPasswordForCurator(100L, 2L));
    }

    private School school(Long id, String name) {
        School school = new School();
        school.setId(id);
        school.setName(name);
        return school;
    }

    private Pupil pupil(Long id, School school) {
        Pupil pupil = new Pupil();
        pupil.setId(id);
        pupil.setEducationalOrganization(school);
        return pupil;
    }
}
