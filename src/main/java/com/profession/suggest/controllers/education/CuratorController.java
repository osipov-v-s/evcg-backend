package com.profession.suggest.controllers.education;

import com.profession.suggest.configuration.security.annotation.HasRole;
import com.profession.suggest.database.entities.auth.role.RoleEnum;
import com.profession.suggest.database.services.education.CuratorService;
import com.profession.suggest.database.services.dataanalys.psychtests.PsychTestService;
import com.profession.suggest.database.services.pupil.PupilService;
import com.profession.suggest.dto.dataanalys.psychtests.AccountTestsDTO;
import com.profession.suggest.dto.education.CuratorDTO;
import com.profession.suggest.dto.education.CuratorUpdateRequestDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.AccountNotFoundException;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/curators")
@RequiredArgsConstructor
public class CuratorController {
    private final CuratorService curatorService;
    private final PupilService pupilService;
    private final PsychTestService psychTestService;

    @HasRole(RoleEnum.CURATOR)
    @GetMapping("/me")
    public ResponseEntity<CuratorDTO> getMyProfile(@RequestAttribute("accountId") Long accountId) {
        return ResponseEntity.ok(curatorService.getProfile(accountId));
    }

    @HasRole(RoleEnum.ADMIN)
    @PutMapping("/{curatorId}")
    public ResponseEntity<CuratorDTO> update(@PathVariable Long curatorId,
                                             @Valid @RequestBody CuratorUpdateRequestDTO request) {
        return ResponseEntity.ok(curatorService.update(curatorId, request));
    }

    @HasRole(RoleEnum.CURATOR)
    @PatchMapping("/pupils/{pupilId}/reset-password")
    public ResponseEntity<Void> resetPupilPassword(@RequestAttribute("accountId") Long accountId,
                                                    @PathVariable Long pupilId)
            throws AccountNotFoundException {
        pupilService.resetPasswordForCurator(accountId, pupilId);
        return ResponseEntity.noContent().build();
    }

    @HasRole(RoleEnum.CURATOR)
    @GetMapping("/pupil-results")
    public ResponseEntity<List<AccountTestsDTO>> getPupilResults(
            @RequestAttribute("accountId") Long accountId,
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {
        Long schoolId = curatorService.getSchoolIdByAccountId(accountId);
        return ResponseEntity.ok(psychTestService
                .getCompletedPupilTestsBySchoolAndDateRange(schoolId, startDate, endDate));
    }
}
