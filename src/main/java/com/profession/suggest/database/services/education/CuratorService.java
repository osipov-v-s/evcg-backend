package com.profession.suggest.database.services.education;

import com.profession.suggest.database.entities.auth.Account;
import com.profession.suggest.database.entities.auth.role.RoleEnum;
import com.profession.suggest.database.entities.education.Curator;
import com.profession.suggest.database.entities.education.School;
import com.profession.suggest.database.repositories.education.CuratorRepository;
import com.profession.suggest.database.services.auth.AccountService;
import com.profession.suggest.dto.education.CuratorCreateRequestDTO;
import com.profession.suggest.dto.education.CuratorDTO;
import com.profession.suggest.dto.education.CuratorUpdateRequestDTO;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CuratorService {
    private final CuratorRepository repository;
    private final SchoolService schoolService;
    private final AccountService accountService;

    @Transactional(readOnly = true)
    public Curator getByAccountId(Long accountId) {
        return repository.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Curator profile not found"));
    }

    @Transactional(readOnly = true)
    public Long getSchoolIdByAccountId(Long accountId) {
        return getByAccountId(accountId).getSchool().getId();
    }

    @Transactional
    public CuratorDTO create(CuratorCreateRequestDTO request) throws BadRequestException {
        School school = schoolService.getById(request.getSchoolId());
        Account account = accountService.registration(request.getAccount(), RoleEnum.CURATOR);

        Curator curator = new Curator();
        curator.setAccount(account);
        curator.setSchool(school);
        curator.setName(request.getName());
        curator.setSurname(request.getSurname());
        curator.setPatronymic(request.getPatronymic());
        return toDTO(repository.save(curator));
    }

    @Transactional(readOnly = true)
    public CuratorDTO getProfile(Long accountId) {
        return toDTO(getByAccountId(accountId));
    }

    @Transactional(readOnly = true)
    public List<CuratorDTO> getBySchoolId(Long schoolId) {
        schoolService.getById(schoolId);
        return repository.findBySchoolId(schoolId).stream().map(this::toDTO).toList();
    }

    @Transactional
    public CuratorDTO update(Long curatorId, CuratorUpdateRequestDTO request) {
        Curator curator = repository.findById(curatorId)
                .orElseThrow(() -> new IllegalArgumentException("Curator not found: " + curatorId));
        curator.setName(request.getName().trim());
        curator.setSurname(request.getSurname().trim());
        curator.setPatronymic(request.getPatronymic());
        curator.setSchool(schoolService.getById(request.getSchoolId()));
        curator.getAccount().setEmail(request.getEmail().trim().toLowerCase());
        accountService.createAccount(curator.getAccount());
        return toDTO(repository.save(curator));
    }

    public CuratorDTO toDTO(Curator curator) {
        return CuratorDTO.builder()
                .id(curator.getId())
                .accountId(curator.getAccount().getId())
                .email(curator.getAccount().getEmail())
                .name(curator.getName())
                .surname(curator.getSurname())
                .patronymic(curator.getPatronymic())
                .school(schoolService.toDTO(curator.getSchool()))
                .build();
    }
}
