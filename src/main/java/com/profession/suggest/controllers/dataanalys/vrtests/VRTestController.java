package com.profession.suggest.controllers.dataanalys.vrtests;

import com.profession.suggest.configuration.security.annotation.HasRole;
import com.profession.suggest.database.entities.auth.Account;
import com.profession.suggest.database.entities.auth.role.RoleEnum;
import com.profession.suggest.database.services.auth.AccountService;
import com.profession.suggest.database.services.dataanalys.vrtests.VRTestService;
import com.profession.suggest.database.services.dataanalys.vrtests.VRTestTypeService;
import com.profession.suggest.dto.dataanalys.TestTypeStatusDTO;
import com.profession.suggest.dto.dataanalys.vrtests.VRTestDTO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.AccountNotFoundException;
import java.util.List;

@RestController
@RequestMapping("/api/vr-tests")
@AllArgsConstructor
@Slf4j
public class VRTestController {
    private final VRTestService vrTestService;
    private final AccountService accountService;
    private final VRTestTypeService vrTestTypeService;

    @HasRole({RoleEnum.PUPIL, RoleEnum.SPECIALIST})
    @PostMapping
    public ResponseEntity<?> createTest(@RequestBody VRTestDTO dto,
                                        @RequestAttribute("accountId") Long accountId){
        try {
            Account account = accountService.getAccountById(accountId);
            vrTestService.prepareTestDto(dto, account);

            VRTestDTO createdTest = vrTestService.createTestWithLimitCheck(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTest);
        } catch (IllegalArgumentException | AccountNotFoundException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());}
    }
    @HasRole({RoleEnum.PUPIL, RoleEnum.SPECIALIST})
    @GetMapping("/my-tests")
    public ResponseEntity<?> getAccountTests(@RequestAttribute("accountId") Long accountId){
        try {
            List<VRTestDTO> tests = vrTestService.getTestsByAccountId(accountId);
            return ResponseEntity.ok(tests);
        } catch (Exception e) {
            log.error("Error fetching tests: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch tests: " + e.getMessage());
        }
    }
    @HasRole({RoleEnum.PUPIL, RoleEnum.SPECIALIST})
    @GetMapping("/my-tests/profession/{professionId}")
    public ResponseEntity<?> getAccountTestsByProfession(
            @RequestAttribute("accountId") Long accountId,
            @PathVariable Long professionId) throws AccountNotFoundException {
        return ResponseEntity.ok(vrTestService.getTestsByAccountIdAndProfessionId(accountId, professionId));
    }
    @HasRole({RoleEnum.PUPIL, RoleEnum.SPECIALIST})
    @DeleteMapping("/my-tests/profession/{professionId}")
    public ResponseEntity<?> resetMyTestsByProfession(
            @RequestAttribute("accountId") Long accountId,
            @PathVariable Long professionId){
        try {
            vrTestService.deleteAllTestsByAccountAndProfession(accountId, professionId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error resetting tests: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to reset tests: " + e.getMessage());
        }
    }

    @HasRole(RoleEnum.ADMIN)
    @GetMapping("/types")
    public ResponseEntity<List<TestTypeStatusDTO>> getTestTypes() {
        return ResponseEntity.ok(vrTestTypeService.getTypes());
    }

    @HasRole(RoleEnum.ADMIN)
    @PatchMapping("/types/{typeId}/active")
    public ResponseEntity<TestTypeStatusDTO> setTestTypeActive(@PathVariable Long typeId,
                                                               @RequestParam boolean active) {
        return ResponseEntity.ok(vrTestTypeService.setActive(typeId, active));
    }



}
