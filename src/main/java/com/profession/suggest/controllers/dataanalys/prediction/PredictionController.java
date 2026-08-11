package com.profession.suggest.controllers.dataanalys.prediction;

import com.profession.suggest.database.services.auth.AccountService;
import com.profession.suggest.database.services.dataanalys.prediction.PredictionService;
import com.profession.suggest.database.services.pupil.PupilService;
import com.profession.suggest.dto.dataanalys.prediction.PredictionDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {
    private final PredictionService predictionService;
    private final AccountService accountService;

    public PredictionController(PredictionService predictionService, AccountService accountService) {
        this.predictionService = predictionService;
        this.accountService = accountService;
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
    @GetMapping("/pupil/{pupilId}")
    public ResponseEntity<List<PredictionDTO>> getPredictionsByPupilId(
            @PathVariable("pupilId") Long pupilId
    ) {
        return ResponseEntity.ok(predictionService.getPredictionsByPupilId(pupilId));
    }
    @PostMapping("/predict")
    public ResponseEntity<?> predict(@RequestAttribute("accountId") Long accountId) {
        try {
            //that only after getting proper results
            return ResponseEntity.ok(
                predictionService.predictByAccount(accountService.getAccountById(accountId)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
    @GetMapping("/latest")
    public ResponseEntity<?> getLatestPrediction(@RequestAttribute("accountId") Long accountId) {
        try {
            return ResponseEntity.ok(predictionService.getLatestPredictionByAccountId(accountId));
        } catch (Exception e) {
            return ResponseEntity.status(404).body("No prediction found");
        }
    }
}
