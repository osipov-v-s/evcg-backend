package com.profession.suggest.controllers.company;

import com.profession.suggest.configuration.security.annotation.HasRole;
import com.profession.suggest.database.entities.auth.role.RoleEnum;
import com.profession.suggest.database.services.specialist.CompanyService;
import com.profession.suggest.dto.company.CompanyDTO;
import com.profession.suggest.dto.company.CompanyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.AccountNotFoundException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/company")
public class CompanyController {
    private final CompanyService companyService;
    private final CompanyMapper companyMapper;

    @HasRole(RoleEnum.ADMIN)
    @GetMapping
    public ResponseEntity<List<CompanyDTO>> getCompanies() {
        return ResponseEntity.ok(companyService.getCompanies());
    }

    @HasRole(RoleEnum.ADMIN)
    @PostMapping
    public ResponseEntity<CompanyDTO> createCompany(@RequestBody CompanyDTO company) {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyService.createCompany(company));
    }

    @HasRole(RoleEnum.ADMIN)
    @PutMapping("/{companyId}")
    public ResponseEntity<CompanyDTO> updateCompany(@PathVariable Long companyId,
                                                     @RequestBody CompanyDTO company) {
        return ResponseEntity.ok(companyService.updateCompany(companyId, company));
    }

    @HasRole(RoleEnum.SPECIALIST)
    @GetMapping("/me")
    public ResponseEntity<CompanyDTO> getMyCompany(@RequestAttribute("accountId") Long accountId)
            throws AccountNotFoundException {
        return ResponseEntity.ok(companyMapper.toDTO(companyService.getCompanyByAccountId(accountId)));
    }
}
