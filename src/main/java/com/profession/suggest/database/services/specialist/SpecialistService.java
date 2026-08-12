package com.profession.suggest.database.services.specialist;

import com.profession.suggest.database.entities.auth.Account;
import com.profession.suggest.database.entities.auth.role.RoleEnum;
import com.profession.suggest.database.entities.gender.Gender;
import com.profession.suggest.database.entities.professions.Profession;
import com.profession.suggest.database.entities.users.specialist.Specialist;
import com.profession.suggest.database.repositories.specialist.SpecialistRepository;
import com.profession.suggest.database.services.auth.AccountService;
import com.profession.suggest.database.services.gender.GenderService;
import com.profession.suggest.database.services.profession.ProfessionService;
import com.profession.suggest.dto.dataanalys.psychtests.PsychTestMapper;
import com.profession.suggest.dto.dataanalys.psychtests.PsychTestDTO;
import com.profession.suggest.dto.specialist.SpecialistCompleteDTO;
import com.profession.suggest.dto.specialist.SpecialistDTO;
import com.profession.suggest.dto.specialist.SpecialistMapper;
import com.profession.suggest.dto.specialist.SpecialistRegisterRequest;
import com.profession.suggest.dto.specialist.SpecialistReferenceDTO;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;

import javax.security.auth.login.AccountNotFoundException;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SpecialistService {
    private final SpecialistRepository repository;
    private final ProfessionService professionService;
    private final SpecialistMapper mapper;
    private final PsychTestMapper psychTestMapper;
    private final GenderService genderService;
    private final AccountService accountService;
    private final CompanyService companyService;
    public SpecialistService(SpecialistRepository repository, ProfessionService professionService, SpecialistMapper mapper, PsychTestMapper psychTestMapper, GenderService genderService, AccountService accountService, CompanyService companyService) {
        this.repository = repository;
        this.professionService = professionService;
        this.mapper = mapper;
        this.psychTestMapper = psychTestMapper;
        this.genderService = genderService;
        this.accountService = accountService;
        this.companyService = companyService;
    }
    public Page<SpecialistDTO> getSpecialistsPage(Pageable pageable,
                                                  String name,
                                                  String profession,
                                                  String company) {
        Specification<Specialist> specification = (root, query, cb) -> cb.conjunction();
        if (name != null && !name.isBlank()) {
            String pattern = "%" + name.trim().toLowerCase() + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("surname")), pattern),
                    cb.like(cb.lower(root.get("patronymic")), pattern)
            ));
        }
        if (profession != null && !profession.isBlank()) {
            String pattern = "%" + profession.trim().toLowerCase() + "%";
            specification = specification.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("profession").get("name")), pattern));
        }
        if (company != null && !company.isBlank()) {
            String pattern = "%" + company.trim().toLowerCase() + "%";
            specification = specification.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("company").get("name")), pattern));
        }
        return repository.findAll(specification, pageable)
                .map(specialist -> mapper.toDTO(specialist, specialist.getAccount()));
    }
    public Page<Specialist> getSpecialists(Pageable pageable, Specification<Specialist> specification) {
        return repository.findAll(specification, pageable);
    }
    //TODO check if profession is null
    public SpecialistDTO create(SpecialistDTO dto, Account account) {
        Specialist specialist = new Specialist();
        if (dto.getProfession() != null && !dto.getProfession().isEmpty()) {
            Profession profession = professionService.getProfessionByName(dto.getProfession());
            specialist.setProfession(profession);
        }
        if (dto.getGender() != null && !dto.getGender().toString().isEmpty()){
            Gender gender = genderService.findGenderByName(dto.getGender());
            specialist.setGender(gender);
        }
        if (dto.getCompanyId() != null)
            specialist.setCompany(companyService.getById(dto.getCompanyId()));

        specialist.setName(dto.getName());
        specialist.setSurname(dto.getSurname());
        specialist.setPatronymic(dto.getPatronymic());
        specialist.setExperience(dto.getExperience());
        specialist.setJobSatisfaction(dto.getJobSatisfaction());
        specialist.setContactPhone(dto.getContactPhone());
        specialist.setContactEmail(dto.getContactEmail());


        specialist.setAccount(account);
        return mapper.toDTO(repository.save(specialist), account);
    }
    public void createAllWithAccounts(List<SpecialistRegisterRequest> specialistsRequests) {
        for (SpecialistRegisterRequest specialistRequest: specialistsRequests) {
            try {
                Account account = accountService.registration(specialistRequest.getAccount(), RoleEnum.SPECIALIST);
                create(specialistRequest.getSpecialist(), account);
            } catch (Exception e) {
                System.err.println("Error processing account ");
            }
        }
    }
    public SpecialistDTO getSpecialistByAccount(Account account) {
        if (account.getSpecialist() == null)
            throw new NullPointerException("Account doesn't have specialist data");
        return mapper.toDTO(account.getSpecialist(), account);
    }
    public SpecialistDTO update(SpecialistDTO specialistDTO) {
        Specialist specialist = repository.findById(specialistDTO.getId())
                .orElseThrow(() -> new NullPointerException("Specialist not found"));
        Account account = specialist.getAccount();
        Gender gender = genderService.findGenderByName(specialistDTO.getGender());
        Profession profession = professionService.getProfessionByName(specialistDTO.getProfession());

        specialist.setName(specialistDTO.getName());
        specialist.setSurname(specialistDTO.getSurname());
        specialist.setPatronymic(specialistDTO.getPatronymic());
        specialist.setExperience(specialistDTO.getExperience());
        specialist.setJobSatisfaction(specialistDTO.getJobSatisfaction());
        specialist.setContactEmail(specialistDTO.getContactEmail());
        specialist.setContactPhone(specialistDTO.getContactPhone());
        if (gender != null)
            specialist.setGender(gender);
        if (profession != null)
            specialist.setProfession(profession);
        if (specialistDTO.getCompanyId() != null)
            specialist.setCompany(companyService.getById(specialistDTO.getCompanyId()));
        return mapper.toDTO(repository.save(specialist), account);
    }
    public SpecialistDTO updateForRequester(SpecialistDTO specialistDTO, Long requesterAccountId)
            throws AccountNotFoundException {
        Account requester = accountService.getAccountById(requesterAccountId);
        boolean admin = requester.getRoles().stream()
                .map(role -> role.getName())
                .anyMatch(role -> role == RoleEnum.ADMIN);
        boolean ownsProfile = requester.getSpecialist() != null
                && requester.getSpecialist().getId().equals(specialistDTO.getId());
        if (!admin && !ownsProfile)
            throw new AccessDeniedException("Cannot update another specialist profile");
        return update(specialistDTO);
    }
    public List<SpecialistCompleteDTO> getCompleteSpecialistsListBetween(LocalDate startDate, LocalDate endDate) {
        List<Specialist> specialists = repository.findByAccountCreatedAtBetween(startDate, endDate);

        return specialists.stream()
                .map(mapper::toCompleteDTO)
                .filter(s -> s.getRoles().contains(RoleEnum.SPECIALIST)) // Added explicit check
                .collect(Collectors.toList());
    }

    @Transactional
    public List<SpecialistReferenceDTO> getPredictionReferenceData() {
        return repository.findAllForPredictionReference().stream()
                .filter(specialist -> specialist.getProfession() != null)
                .filter(specialist -> specialist.getAccount().getRoles().stream()
                        .map(role -> role.getName())
                        .anyMatch(role -> role == RoleEnum.SPECIALIST))
                .map(specialist -> {
                    Map<String, PsychTestDTO> recentTests = specialist.getPsychTests().stream()
                            .filter(test -> test.getPsychTestType() != null)
                            .collect(Collectors.toMap(
                                    test -> test.getPsychTestType().getName(),
                                    psychTestMapper::toDTO,
                                    this::newerTest,
                                    LinkedHashMap::new));
                    return new SpecialistReferenceDTO(
                            specialist.getId(),
                            specialist.getProfession().getName(),
                            recentTests);
                })
                .collect(Collectors.toList());
    }

    private PsychTestDTO newerTest(PsychTestDTO existing, PsychTestDTO replacement) {
        if (existing.getCreatedAt() == null) return replacement;
        if (replacement.getCreatedAt() == null) return existing;
        return existing.getCreatedAt().isAfter(replacement.getCreatedAt()) ? existing : replacement;
    }
    public Specialist getSpecialistById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No specialist with id " + id));
    }
    public Specialist save(Specialist specialist) {
        return repository.save(specialist);
    }
}
