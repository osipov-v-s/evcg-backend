package com.profession.suggest.controllers.dataanalys.psychtests;

import com.profession.suggest.configuration.security.annotation.HasRole;
import com.profession.suggest.database.entities.auth.role.RoleEnum;
import com.profession.suggest.database.services.auth.AccountService;
import com.profession.suggest.database.services.dataanalys.psychtests.PsychTestService;
import com.profession.suggest.database.services.dataanalys.psychtests.PsychTestTypeService;
import com.profession.suggest.database.services.pupil.PupilService;
import com.profession.suggest.dto.dataanalys.psychtests.AccountTestsDTO;
import com.profession.suggest.dto.dataanalys.psychtests.PsychTestDTO;
import com.profession.suggest.dto.dataanalys.TestTypeStatusDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.AccountNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/psych-tests")
public class PsychTestsController {
    private final PsychTestService psychTestService;
    //private final PsychTestTypeService psychTestTypeService; admin etc
    private final AccountService accountService;
    private final PsychTestTypeService psychTestTypeService;
    public PsychTestsController(PsychTestService psychTestService, AccountService accountService, PsychTestTypeService psychTestTypeService) {
        this.psychTestService = psychTestService;
        this.accountService = accountService;
        this.psychTestTypeService = psychTestTypeService;
    }
    @HasRole(RoleEnum.ADMIN)
    @GetMapping("/types")
    public ResponseEntity<List<TestTypeStatusDTO>> getTestTypes() {
        return ResponseEntity.ok(psychTestTypeService.getTypes());
    }

    @HasRole(RoleEnum.ADMIN)
    @PatchMapping("/types/{typeId}/active")
    public ResponseEntity<TestTypeStatusDTO> setTestTypeActive(@PathVariable Long typeId,
                                                               @RequestParam boolean active) {
        return ResponseEntity.ok(psychTestTypeService.setActive(typeId, active));
    }
    //TODO create test for pupil and specialist
    @HasRole({RoleEnum.PUPIL, RoleEnum.SPECIALIST})
    @PostMapping("/create-test")
    public ResponseEntity<PsychTestDTO> createPsychTest(@RequestBody PsychTestDTO requestDTO, @RequestAttribute("accountId") Long accountId) throws AccountNotFoundException {
        return ResponseEntity.ok(
                psychTestService.createPsychTest(
                        requestDTO,
                        accountService.getAccountById(accountId)));
    }
    @HasRole({RoleEnum.PUPIL, RoleEnum.SPECIALIST})
    @GetMapping("/my-tests")
    public ResponseEntity<List<PsychTestDTO>> getTestsForAccount(@RequestAttribute("accountId") Long accountId) throws AccountNotFoundException {
        return ResponseEntity.ok(psychTestService.getTestsResultsByAccount(accountService.getAccountById(accountId)));
    }
    @HasRole({RoleEnum.PUPIL, RoleEnum.SPECIALIST})
    @GetMapping("/my-recent-tests")
    public ResponseEntity<Map<String, PsychTestDTO>> getAccountRecentTests(@RequestAttribute("accountId") Long accountId) throws AccountNotFoundException {
        return ResponseEntity.ok(psychTestService.getAccountRecentTests(accountService.getAccountById(accountId)));
    }
    @HasRole({RoleEnum.PUPIL, RoleEnum.SPECIALIST})
    @GetMapping("/my-tests/type/{testType}")
    public ResponseEntity<List<PsychTestDTO>> getTestsByType(@RequestAttribute("accountId") Long accountId, @PathVariable String testType) throws AccountNotFoundException {
        return ResponseEntity.ok(psychTestService.getAccountTestsByType(accountService.getAccountById(accountId), testType));
    }
    @HasRole(RoleEnum.ADMIN)
    @GetMapping("/completed-tests")
    public ResponseEntity<List<AccountTestsDTO>> getCompletedTestsByDates(@RequestParam("type") String type,
                                                                          @RequestParam("startDate") LocalDateTime startDate,
                                                                          @RequestParam("endDate") LocalDateTime endDate) {
        try {
            //List<PsychTestDTO> testDTOS = psychTestService.getCompletedTestsByDateRange(type, startDate, endDate);
            return ResponseEntity.ok(psychTestService.getCompletedTestsByDateRange(type, startDate, endDate));
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }


}
