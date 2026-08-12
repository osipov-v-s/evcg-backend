package com.profession.suggest.database.services.pupil;

import com.profession.suggest.database.entities.auth.Account;
import com.profession.suggest.database.entities.auth.role.Role;
import com.profession.suggest.database.entities.auth.role.RoleEnum;
import com.profession.suggest.database.entities.gender.Gender;
import com.profession.suggest.database.entities.gender.GenderEnum;
import com.profession.suggest.database.entities.education.School;
import com.profession.suggest.database.entities.users.pupil.Pupil;
import com.profession.suggest.database.repositories.pupil.PupilRepository;
import com.profession.suggest.database.services.auth.AccountService;
import com.profession.suggest.database.services.auth.role.RoleService;
import com.profession.suggest.database.services.gender.GenderService;
import com.profession.suggest.database.services.education.CuratorService;
import com.profession.suggest.database.services.education.SchoolService;
import com.profession.suggest.dto.auth.AccountApiRegisterDTO;
import com.profession.suggest.dto.auth.AccountMapper;
import com.profession.suggest.dto.auth.AccountRegisterRequestDTO;
import com.profession.suggest.dto.pupil.PupilCompleteDTO;
import com.profession.suggest.dto.pupil.PupilDTO;
import com.profession.suggest.dto.pupil.PupilMapper;
import com.profession.suggest.dto.pupil.PupilResponseDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;

import javax.security.auth.login.AccountNotFoundException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PupilService {
    private final PupilRepository repository;
    private final AccountService accountService;
    private final PasswordEncoder passwordEncoder;
    private final GenderService genderService;
    private final RoleService roleService;
    private final PupilMapper pupilMapper;
    private final AccountMapper accountMapper;
    private final SchoolService schoolService;
    private final CuratorService curatorService;
    @Value("${account.default.password}")
    private String defaultPassword;
    @Value("${pupil.default.name:Ivan}")
    private String defaultName;
    @Value("${pupil.default.second-name:Ivanov}")
    private String defaultSecondName;
    @Value("${pupil.default.patronymic:Ivanovich}")
    private String defaultPatronymic;
    @Value("${pupil.default.gender:MALE}")
    private GenderEnum defaultGender;
    public Pupil getPupilById(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Cant find pupil with id " + id));
    }
    public Pupil getPupilByAccountId(Long accountId) {
        return repository.findByAccountId(accountId);
    }
    public Pupil create(Pupil pupil) {
        return repository.save(pupil);
    }
    @Transactional
    public PupilDTO createWithAccount(AccountApiRegisterDTO accountApiRegisterDTO) throws BadRequestException {
        Pupil pupil = pupilMapper.fromDTO(accountApiRegisterDTO.getPupilDTO());
        Account account = accountMapper.fromDTO(accountApiRegisterDTO.getAccountRegisterRequestDTO());
        account.setPassword(passwordEncoder.encode(account.getPassword()));
        Role role = roleService.findByName(RoleEnum.PUPIL);
        if (account.getRoles() == null) account.setRoles(new HashSet<>());
        account.getRoles().add(role);
        Account savedAccount = accountService.registration(accountApiRegisterDTO.getAccountRegisterRequestDTO(), RoleEnum.PUPIL);
        Gender gender = genderService.findGenderByName(accountApiRegisterDTO.getPupilDTO().getGender());
        pupil.setGender(gender);
        pupil.setAccount(savedAccount);
        applyEducationalOrganization(pupil, accountApiRegisterDTO.getPupilDTO());

        Pupil savedPupil = repository.save(pupil);
        return pupilMapper.toDTO(savedPupil);
    }

    public void createAllWithAccounts(List<AccountApiRegisterDTO> accounts) {
        for (AccountApiRegisterDTO account: accounts) {
            try {
                createWithAccount(account);
            } catch (Exception e) {
                System.err.println("Error processing account: " + e.getMessage());
            }
        }
    }
    //3 outcomes
    //1 Has account but not pupil
    //2 Has account and pupil
    //3 Doesnt have account and pupil
    @Transactional
    public Pupil createWithDefaults(String email) throws AccountNotFoundException, BadRequestException {

        Pupil existingPupil = repository.findByAccountEmail(email).orElse(null);
        //Has account and pupil
        if (existingPupil != null) return existingPupil;
        if (accountService.isEmailFree(email)) return createDefaultPupilWithNewAccount(email);
        return createDefaultPupil(accountService.getAccountByEmail(email));

    }
    public Pupil createDefaultPupil(Account account) {
        Pupil pupil = new Pupil();
        Gender gender = genderService.findGenderByName(defaultGender);
        pupil.setName(defaultName);
        pupil.setSurname(defaultSecondName);
        pupil.setPatronymic(defaultPatronymic);
        pupil.setGender(gender);
        pupil.setAccount(account);
        return repository.save(pupil);
    }
    public Pupil createDefaultPupilWithNewAccount(String email) throws BadRequestException {
        AccountApiRegisterDTO registerDTO = new AccountApiRegisterDTO();

        // Setup pupil DTO
        PupilDTO pupilDTO = new PupilDTO();
        pupilDTO.setGender(defaultGender);
        pupilDTO.setName(defaultName);
        pupilDTO.setSurname(defaultSecondName);
        pupilDTO.setPatronymic(defaultPatronymic);
        registerDTO.setPupilDTO(pupilDTO);

        // Setup account DTO
        AccountRegisterRequestDTO accountDTO = new AccountRegisterRequestDTO();
        accountDTO.setEmail(email);
        accountDTO.setPassword(defaultPassword);
        registerDTO.setAccountRegisterRequestDTO(accountDTO);

        createWithAccount(registerDTO);
        return repository.findByAccountEmail(email)
                .orElseThrow(() -> new RuntimeException("Cannot find pupil by email: " + email));
    }
    public PupilDTO updatePupilData(PupilDTO pupilDTO, Long accountId) throws AccountNotFoundException {
        Pupil pupil = getPupilByAccountId(accountId);
        Account account = accountService.getAccountById(accountId);
        Gender gender = genderService.findGenderByName(pupilDTO.getGender());
        if (pupil == null) {
            pupil = new Pupil();
            pupil.setAccount(account);
        }
        pupil = pupilMapper.updateFromDTO(pupil, pupilDTO);
        applyEducationalOrganization(pupil, pupilDTO);
        pupil.setGender(gender);
        return pupilMapper.toDTO(repository.save(pupil));
    }
    public Page<PupilResponseDTO> getPupilsData(Long requesterAccountId,
                                                Pageable pageable,
                                                String school,
                                                String email,
                                                String name,
                                                Integer classNumber,
                                                GenderEnum gender) throws AccountNotFoundException {
        Specification<Pupil> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        var roles = accountService.getActiveRoleNamesByAccount(requesterAccountId);

        if (roles.contains(RoleEnum.CURATOR)) {
            Long schoolId = curatorService.getSchoolIdByAccountId(requesterAccountId);
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("educationalOrganization").get("id"), schoolId));
        } else if (!roles.contains(RoleEnum.ADMIN)) {
            throw new AccessDeniedException("Only ADMIN or CURATOR can list pupils");
        }

        if (school != null && !school.isBlank() && roles.contains(RoleEnum.ADMIN)) {
            String pattern = "%" + school.trim().toLowerCase() + "%";
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("school")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("educationalOrganization").get("name")), pattern)
            ));
        }
        if (email != null && !email.isBlank()) {
            String pattern = "%" + email.trim().toLowerCase() + "%";
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("account").get("email")), pattern));
        }
        if (name != null && !name.isBlank()) {
            String pattern = "%" + name.trim().toLowerCase() + "%";
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("surname")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("patronymic")), pattern)
            ));
        }
        if (classNumber != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("classNumber"), classNumber));
        }
        if (gender != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("gender").get("name"), gender));
        }

        return repository.findAll(specification, pageable).map(pupilMapper::toResponseDTO);
    }
    public PupilResponseDTO getPupilData(Long id) {
        return repository.findPupilData(id);
    }
    public Optional<Pupil> getPupilByAccountEmail(String email) {
        return repository.findByAccountEmail(email);
    }
    public List<PupilCompleteDTO> getCompletePupilsListBetween(Long requesterAccountId,
                                                               LocalDate startDate,
                                                               LocalDate endDate) throws AccountNotFoundException {
        var roles = accountService.getActiveRoleNamesByAccount(requesterAccountId);
        List<Pupil> pupils;
        if (roles.contains(RoleEnum.CURATOR)) {
            Long schoolId = curatorService.getSchoolIdByAccountId(requesterAccountId);
            pupils = repository.findByEducationalOrganizationIdAndAccountCreatedAtBetween(schoolId, startDate, endDate);
        } else if (roles.contains(RoleEnum.ADMIN)) {
            pupils = repository.findByAccountCreatedAtBetween(startDate, endDate);
        } else {
            throw new AccessDeniedException("Only ADMIN or CURATOR can access pupil reports");
        }

        return pupils.stream()
                .map(pupilMapper::toCompleteDTO)
                .collect(Collectors.toList());
    }

    public PupilResponseDTO getPupilDataForRequester(Long requesterAccountId, Long pupilId)
            throws AccountNotFoundException {
        Pupil pupil = getPupilById(pupilId);
        var roles = accountService.getActiveRoleNamesByAccount(requesterAccountId);
        if (roles.contains(RoleEnum.ADMIN))
            return pupilMapper.toResponseDTO(pupil);
        if (roles.contains(RoleEnum.CURATOR)) {
            Long curatorSchoolId = curatorService.getSchoolIdByAccountId(requesterAccountId);
            Long pupilSchoolId = pupil.getEducationalOrganization() != null
                    ? pupil.getEducationalOrganization().getId()
                    : null;
            if (curatorSchoolId.equals(pupilSchoolId))
                return pupilMapper.toResponseDTO(pupil);
        }
        throw new AccessDeniedException("Pupil is outside curator educational organization");
    }

    public PupilDTO assignSchool(Long pupilId, Long schoolId) {
        Pupil pupil = getPupilById(pupilId);
        School school = schoolService.getById(schoolId);
        pupil.setEducationalOrganization(school);
        pupil.setSchool(school.getName());
        return pupilMapper.toDTO(repository.save(pupil));
    }

    @Transactional
    public void resetPasswordForCurator(Long requesterAccountId, Long pupilId)
            throws AccountNotFoundException {
        getPupilDataForRequester(requesterAccountId, pupilId);
        Pupil pupil = getPupilById(pupilId);
        if (pupil.getAccount() == null)
            throw new IllegalArgumentException("Pupil account not found");
        pupil.getAccount().setPassword(passwordEncoder.encode(defaultPassword));
        pupil.getAccount().setFirstLogin(true);
        accountService.createAccount(pupil.getAccount());
    }

    private void applyEducationalOrganization(Pupil pupil, PupilDTO dto) {
        School school = null;
        if (dto.getSchoolId() != null)
            school = schoolService.getById(dto.getSchoolId());
        else if (dto.getSchool() != null)
            school = schoolService.findByName(dto.getSchool());

        if (school != null) {
            pupil.setEducationalOrganization(school);
            pupil.setSchool(school.getName());
        }
    }
}
