package com.profession.suggest.controllers.auth;

import com.profession.suggest.configuration.security.annotation.HasRole;
import com.profession.suggest.database.entities.auth.role.RoleEnum;
import com.profession.suggest.database.services.auth.AccountService;
import com.profession.suggest.database.services.pupil.PupilService;
import com.profession.suggest.dto.auth.AccountApiRegisterDTO;
import com.profession.suggest.dto.auth.AccountDTO;
import com.profession.suggest.dto.auth.AccountRegisterRequestDTO;
import com.profession.suggest.dto.auth.RoleDTO;
import com.profession.suggest.dto.pupil.PupilDTO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.AccountNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {
    private final PupilService pupilService;
    private final AccountService accountService;

    public AuthController(PupilService pupilService, AccountService accountService) {
        this.pupilService = pupilService;
        this.accountService = accountService;
    }
    //Legacy, this is PupilController method
    @PostMapping("/auto-register")
    public ResponseEntity<PupilDTO> autoRegister(@Valid @RequestBody AccountApiRegisterDTO accountApiRegisterDTO) throws BadRequestException {
        return ResponseEntity.ok(pupilService.createWithAccount(accountApiRegisterDTO));
    }
    //Legacy, this is PupilController method and PupilApiRegisterDTO best name as in SpecialistController
    @PostMapping("/auto-register-all")
    @HasRole(RoleEnum.ADMIN)
    public ResponseEntity<String> autoRegisterAll(@RequestBody List<AccountApiRegisterDTO> accountApiRegisterDTOList) {
        pupilService.createAllWithAccounts(accountApiRegisterDTOList);
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody AccountRegisterRequestDTO account) throws BadRequestException {
        try {
            return ResponseEntity.ok(accountService.registration(account, RoleEnum.PUPIL).getEmail());
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("please check all fields");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("email already in use");
        }

    }
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody AccountDTO account) {
        try {
            return ResponseEntity.ok(accountService.login(account));
        } catch (AccountNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("please check login or password");
        }

    }
    @GetMapping("/account-roles")
    public ResponseEntity<List<RoleDTO>> getAccountRoles(@RequestAttribute("accountId") Long accountId) throws AccountNotFoundException {
        try {
            return ResponseEntity.ok(accountService.getActiveRoleNamesByAccount(accountId).stream()
                    .map(RoleDTO::new)
                    .collect(Collectors.toList()));
        } catch (AccountNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

    }
    @PostMapping("/update-password")
    public ResponseEntity<?> updatePassword(@RequestAttribute("accountId") Long accountId,
                                            @RequestBody AccountDTO accountDTO) {
        try {
            accountService.updatePassword(accountId, accountDTO.getPassword());
            return ResponseEntity.ok().body(Map.of("message", "Password updated successfully"));
        } catch (AccountNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Account not found"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating password for account {}", accountId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error occurred while updating password"));
        }
    }
    @GetMapping("/is-email-free")
    public ResponseEntity<Boolean> isEmailFree(@RequestParam String email ) {
        try {
            return ResponseEntity.ok(accountService.isEmailFree(email));
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}
