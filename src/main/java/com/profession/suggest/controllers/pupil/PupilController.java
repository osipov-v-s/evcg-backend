package com.profession.suggest.controllers.pupil;

import com.profession.suggest.configuration.security.annotation.HasRole;
import com.profession.suggest.database.entities.auth.role.RoleEnum;
import com.profession.suggest.database.entities.gender.GenderEnum;
import com.profession.suggest.database.services.auth.AccountService;
import com.profession.suggest.database.services.dataanalys.prediction.PredictionService;
import com.profession.suggest.database.services.pupil.PupilService;
import com.profession.suggest.dto.dataanalys.prediction.PredictionDTO;
import com.profession.suggest.dto.pupil.PupilDTO;
import com.profession.suggest.dto.pupil.PupilResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.AccountNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * TODO
 * 1. get pupil values by token +
 * 2. get all pupil tests data by token
 * */
@RestController
@RequestMapping("/api/pupils")
public class PupilController {

    private final PupilService pupilService;
    private final AccountService accountService;
    private final PredictionService predictionService;

    private static final List<String> ALLOWED_SORT_FIELDS = List.of(
            "id", "name", "surname", "createdAt", "classNumber"
    );

    public PupilController(PupilService pupilService, AccountService accountService, PredictionService predictionService) {
        this.pupilService = pupilService;
        this.accountService = accountService;
        this.predictionService = predictionService;
    }

    @HasRole({RoleEnum.ADMIN, RoleEnum.CURATOR})
    @GetMapping()
    public ResponseEntity<Page<PupilResponseDTO>> getAllValidPupilWithAccounts(
            @RequestAttribute("accountId") Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String school,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer classNumber,
            @RequestParam(required = false) GenderEnum gender) throws AccountNotFoundException {
        if (!ALLOWED_SORT_FIELDS.contains(sortBy))
            sortBy = "id";
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy));
        return ResponseEntity.ok(pupilService.getPupilsData(
                accountId, pageable, school, email, name, classNumber, gender));
    }
    @HasRole(RoleEnum.PUPIL)
    @GetMapping("/pupil-data")
    public ResponseEntity<PupilResponseDTO> getPupilDataByAccount(@RequestAttribute("accountId") Long accountId) {
        return ResponseEntity.ok(accountService.getPupilDataByAccountId(accountId));
    }
    @HasRole(RoleEnum.PUPIL)
    @PostMapping("/update-pupil-data")
    public ResponseEntity<PupilDTO> updatePupilData(@RequestBody PupilDTO dto , @RequestAttribute("accountId") Long accountId) throws AccountNotFoundException {
        return ResponseEntity.ok(pupilService.updatePupilData(dto, accountId));
    }
    @HasRole({RoleEnum.ADMIN, RoleEnum.CURATOR})
    @GetMapping("/completed-tests")
    public ResponseEntity<?> getCompletedTestsByDates(@RequestAttribute("accountId") Long accountId,
                                                       @RequestParam("startDate") LocalDate startDate,
                                                                           @RequestParam("endDate") LocalDate endDate) {
        try{
            return ResponseEntity.ok(pupilService.getCompletePupilsListBetween(accountId, startDate, endDate));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Failed to fetch pupils data",
                            "message", e.getMessage(),
                            "cause", e.getCause() != null ? e.getCause().getMessage() : "Unknown"
                    ));
        }

    }
    @HasRole({RoleEnum.ADMIN, RoleEnum.CURATOR})
    @GetMapping("/{pupilId}")
    public ResponseEntity<PupilResponseDTO> getPupil(@RequestAttribute("accountId") Long accountId,
                                                      @PathVariable Long pupilId) throws AccountNotFoundException {
        return ResponseEntity.ok(pupilService.getPupilDataForRequester(accountId, pupilId));
    }

    @HasRole(RoleEnum.ADMIN)
    @PatchMapping("/{pupilId}/school/{schoolId}")
    public ResponseEntity<PupilDTO> assignSchool(@PathVariable Long pupilId,
                                                  @PathVariable Long schoolId) {
        return ResponseEntity.ok(pupilService.assignSchool(pupilId, schoolId));
    }

    @HasRole(RoleEnum.PUPIL)
    @GetMapping("/pupil/predictions")
    public ResponseEntity<List<PredictionDTO>> getPupilPredictions(@RequestAttribute("accountId") Long accountId) {
        return ResponseEntity.ok(
                predictionService.getPredictionsByPupilId(
                        pupilService.getPupilByAccountId(accountId).getId()));

    }

}
