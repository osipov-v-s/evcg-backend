package com.profession.suggest.database.services.dataanalys.psychtests;

import com.profession.suggest.database.entities.auth.Account;
import com.profession.suggest.database.entities.auth.role.Role;
import com.profession.suggest.database.entities.dataanalys.psychtests.PsychParam;
import com.profession.suggest.database.entities.dataanalys.psychtests.PsychTest;
import com.profession.suggest.database.entities.dataanalys.psychtests.PsychTestType;
import com.profession.suggest.database.entities.users.User;
import com.profession.suggest.database.entities.users.pupil.Pupil;
import com.profession.suggest.database.entities.users.specialist.Specialist;
import com.profession.suggest.database.repositories.dataanalys.psychtests.PsychTestRepository;
import com.profession.suggest.database.services.pupil.PupilService;
import com.profession.suggest.dto.dataanalys.psychtests.AccountTestsDTO;
import com.profession.suggest.dto.dataanalys.psychtests.PsychTestDTO;
import com.profession.suggest.dto.dataanalys.psychtests.PsychTestMapper;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@AllArgsConstructor
public class PsychTestService {
    private final PsychTestRepository repository;
    private final PsychParamService psychParamService;
    private final PupilService pupilService;
    private final PsychTestTypeService psychTestTypeService;
    private final PsychTestMapper mapper;

    @Transactional
    public PsychTestDTO createPsychTest(PsychTestDTO psychTestDTO, Account account) {
        validateAccountRoles(account);
        validateRequest(psychTestDTO);

        PsychTestType psychTestType = psychTestTypeService.getPsychTestTypeByName(psychTestDTO.getTestTypeName());
        PsychTest psychTest = mapper.fromDTO(psychTestDTO);
        Set<PsychParam> params = new HashSet<>();
        for (PsychParam param : psychTest.getPsychParams()) {
            PsychParam savedParam = psychParamService.create(param);
            params.add(savedParam);
        }
        psychTest.setPsychParams(params);
        psychTest.setPsychTestType(psychTestType);
        psychTest.setPupil(account.getPupil());
        psychTest.setSpecialist(account.getSpecialist());

        return mapper.toDTO(repository.save(psychTest));
    }
    @Deprecated
    public List<PsychTestDTO> getPupilTests(Pupil pupil) {
        List<PsychTest> psychTests = repository.findByPupil(pupil);
        return psychTests.stream().map( test -> {
            try {
                return mapper.toDTO(test);
            } catch (NullPointerException e) {
                return null;
            }
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    }
    public List<PsychTestDTO> getTestsResultsByAccount(Account account) {
        validateAccountRoles(account);
        List<PsychTest> psychTests = repository.findByAccountId(account.getId());
        return psychTests.stream().filter(test -> test.getPsychTestType() != null).
                map(mapper::toDTO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    @Deprecated
    public List<PsychTestDTO> getPupilTestByType(Pupil pupil, String testTypeName) {
        PsychTestType testType = psychTestTypeService.getPsychTestTypeByName(testTypeName);
        List<PsychTest> psychTests = repository.findByPupilAndPsychTestType(pupil, testType);
        return psychTests.stream().map(mapper::toDTO).collect(Collectors.toList());
    }
    public List<PsychTestDTO> getAccountTestsByType(Account account, String testTypeName) {
        validateAccountRoles(account);
        List<PsychTest> psychTests = repository.findByAccountIdAndTestType(account.getId(), testTypeName);
        return psychTests.stream().map(mapper::toDTO).collect(Collectors.toList());
    }
    public List<AccountTestsDTO> getCompletedTestsByDateRange(String type, LocalDateTime startDate, LocalDateTime endDate) {
        List<PsychTest> psychTests;
        if (Objects.equals(type, "Pupil"))
            psychTests = repository.findByPupilAndDateRange(startDate, endDate);
        else if (Objects.equals(type, "Specialist"))
            psychTests = repository.findBySpecialistAndDateRange(startDate, endDate);
        else
            psychTests = repository.findByDateRange(startDate, endDate);
        return mapTests(psychTests);
    }

    public List<AccountTestsDTO> getCompletedPupilTestsBySchoolAndDateRange(Long schoolId,
                                                                             LocalDateTime startDate,
                                                                             LocalDateTime endDate) {
        return mapTests(repository.findByPupilSchoolAndDateRange(schoolId, startDate, endDate));
    }

    private List<AccountTestsDTO> mapTests(List<PsychTest> psychTests) {
        Map<Long, AccountTestsDTO> accountMap = new LinkedHashMap<>();
        for (PsychTest test: psychTests) {
            User user = test.getPupil() != null ? test.getPupil() : test.getSpecialist();
            if (user == null) continue;
            Long accountId = user.getAccount().getId();

            AccountTestsDTO accountDTO = accountMap.computeIfAbsent(accountId,
                    id -> {
                        AccountTestsDTO dto = new AccountTestsDTO();
                        dto.setAccountId(accountId);
                        dto.setEmail(user.getAccount().getEmail());
                        dto.setName(user.getName());
                        dto.setSurname(user.getSurname());
                        dto.setPatronymic(user.getPatronymic());
                        dto.setFullName(user.getFullName());
                        dto.setRoles(user.getAccount().getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
                        dto.setPsychTests(new ArrayList<>());
                        if (user instanceof Pupil pupil) {
                            dto.setSchool(pupil.getEducationalOrganization() != null
                                    ? pupil.getEducationalOrganization().getName()
                                    : pupil.getSchool());
                            dto.setClassNumber(pupil.getClassNumber());
                            dto.setClassLabel(pupil.getClassLabel());
                            dto.setGender(pupil.getGender() != null ? pupil.getGender().getName().name() : null);
                            dto.setBirthday(pupil.getBirthday());
                        } else if (user instanceof Specialist specialist) {
                            dto.setProfession(specialist.getProfession() != null
                                    ? specialist.getProfession().getName()
                                    : null);
                            dto.setCompany(specialist.getCompany() != null
                                    ? specialist.getCompany().getName()
                                    : null);
                        }
                        return dto;
                    });
            accountDTO.getPsychTests().add(mapper.toDTO(test));
        }
        return new ArrayList<>(accountMap.values());
    }
    /**
     * Search tests only for the most recent date and only one testType
     * Works good if tests < 1000 mean always
     * */
    public Map<String, PsychTestDTO> getAccountRecentTests(Account account) {
        validateAccountRoles(account);
        List<PsychTest> psychTests = repository.findByAccountId(account.getId()).stream()
                .filter(test -> test.getPsychTestType() != null)
                .toList();
        return psychTests.stream().collect(Collectors.toMap(
                test -> test.getPsychTestType().getName(),
                test -> mapper.toDTO(test),
                (existing, replacement) -> existing.getCreatedAt().isAfter(replacement.getCreatedAt()) ? existing : replacement
        ));
    }
    private void validateAccountRoles(Account account) {
        Pupil pupil = account.getPupil();
        Specialist specialist = account.getSpecialist();
        if (pupil == null && specialist == null) {
            throw new IllegalArgumentException("Account has no role assigned (neither Pupil nor Specialist)");
        }
        if (pupil != null && specialist != null) {
            throw new IllegalArgumentException("Account has both Pupil and Specialist roles - ambiguous");
        }
    }
    private void validateRequest(PsychTestDTO psychTestDTO) {
        if (psychTestDTO.getPsychParams() == null)
            throw new IllegalArgumentException("psychParams list is null or empty");
        if (psychTestDTO.getTestTypeName() == null || psychTestDTO.getTestTypeName().isBlank())
            throw new IllegalArgumentException("TestTypeName is null");
    }
}
