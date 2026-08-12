package com.profession.suggest.controllers.dataanalys.prediction;

import com.profession.suggest.configuration.security.annotation.HasRole;
import com.profession.suggest.database.entities.auth.role.RoleEnum;
import com.profession.suggest.database.services.auth.AccountService;
import com.profession.suggest.database.services.dataanalys.prediction.PredictionService;
import com.profession.suggest.database.services.pupil.PupilService;
import com.profession.suggest.dto.dataanalys.prediction.PredictionDTO;
import com.profession.suggest.dto.dataanalys.prediction.PredictionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {
    private final PredictionService predictionService;
    private final AccountService accountService;
    private final PupilService pupilService;

    public PredictionController(PredictionService predictionService, AccountService accountService, PupilService pupilService) {
        this.predictionService = predictionService;
        this.accountService = accountService;
        this.pupilService = pupilService;
    }
    //Accept predictions from service, DEPRECATED
    /*
    @PostMapping("/create")
    public ResponseEntity<?> createPrediction(@RequestPart("prediction") PredictionDTO predictionDTO,
                                                          @RequestPart("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(predictionService.createPrediction(predictionDTO, file));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Please check all required parameters, cant save prediction");
        }
    }
    */
    @HasRole({RoleEnum.ADMIN, RoleEnum.CURATOR})
    @GetMapping("/pupil/{pupilId}")
    public ResponseEntity<List<PredictionDTO>> getPredictionsByPupilId(
            @RequestAttribute("accountId") Long accountId,
            @PathVariable("pupilId") Long pupilId
    ) throws Exception {
        pupilService.getPupilDataForRequester(accountId, pupilId);
        return ResponseEntity.ok(predictionService.getPredictionsByPupilId(pupilId));
    }
    @HasRole(RoleEnum.PUPIL)
    @PostMapping("/predict")
    public ResponseEntity<PredictionResponse> predict(@RequestAttribute("accountId") Long accountId)
            throws Exception {
        return ResponseEntity.ok(
                predictionService.predictByAccount(accountService.getAccountById(accountId)));
    }
    @HasRole(RoleEnum.PUPIL)
    @GetMapping("/latest")
    public ResponseEntity<?> getLatestPrediction(@RequestAttribute("accountId") Long accountId) {
        try {
            return ResponseEntity.ok(predictionService.getLatestPredictionByAccountId(accountId));
        } catch (Exception e) {
            return ResponseEntity.status(404).body("No prediction found");
        }
    }
}
