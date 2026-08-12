package com.profession.suggest.database.services.specialist;

import com.profession.suggest.database.entities.auth.Account;
import com.profession.suggest.database.entities.users.specialist.Company;
import com.profession.suggest.database.entities.users.specialist.Specialist;
import com.profession.suggest.database.repositories.specialist.CompanyRepository;
import com.profession.suggest.database.services.auth.AccountService;
import com.profession.suggest.dto.company.CompanyDTO;
import com.profession.suggest.dto.company.CompanyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.security.auth.login.AccountNotFoundException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyService {
    private final CompanyRepository repository;
    private final AccountService accountService;
    private final CompanyMapper companyMapper;

    @Transactional(readOnly = true)
    public Company getCompanyByAccountId(Long accountId) throws AccountNotFoundException {
        Account account = accountService.getAccountById(accountId);
        Specialist specialist = account.getSpecialist();
        if (specialist == null || specialist.getCompany() == null)
            throw new IllegalArgumentException("No company assigned to this specialist");
        return specialist.getCompany();
    }

    @Transactional(readOnly = true)
    public List<CompanyDTO> getCompanies() {
        return repository.findAll().stream().map(companyMapper::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public Company getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + id));
    }

    @Transactional
    public CompanyDTO createCompany(CompanyDTO companyDTO) {
        if (companyDTO.getName() == null || companyDTO.getName().isBlank())
            throw new IllegalArgumentException("Company name is required");
        if (repository.findByName(companyDTO.getName().trim()) != null)
            throw new IllegalArgumentException("Company already exists: " + companyDTO.getName());

        Company company = new Company();
        apply(company, companyDTO);
        return companyMapper.toDTO(repository.save(company));
    }

    @Transactional
    public CompanyDTO updateCompany(Long id, CompanyDTO companyDTO) {
        Company company = getById(id);
        apply(company, companyDTO);
        return companyMapper.toDTO(repository.save(company));
    }

    private void apply(Company company, CompanyDTO dto) {
        if (dto.getName() != null && !dto.getName().isBlank())
            company.setName(dto.getName().trim());
        company.setInn(dto.getInn());
        company.setOgrn(dto.getOgrn());
        company.setAddress(dto.getAddress());
        company.setPhone(dto.getPhone());
        company.setEmail(dto.getEmail());
    }
}
