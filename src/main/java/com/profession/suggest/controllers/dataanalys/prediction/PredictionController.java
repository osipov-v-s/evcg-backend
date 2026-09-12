package com.profession.suggest.controllers.dataanalys.prediction;

import com.profession.suggest.configuration.security.annotation.HasRole;
import com.profession.suggest.database.entities.auth.role.RoleEnum;
import com.profession.suggest.database.entities.dataanalys.prediction.Prediction;
import com.profession.suggest.database.entities.dataanalys.prediction.math.MathPrediction;
import com.profession.suggest.database.services.auth.AccountService;
import com.profession.suggest.database.services.dataanalys.prediction.ClusterPredictionService;
import com.profession.suggest.database.services.dataanalys.prediction.MathPredictionService;
import com.profession.suggest.database.services.dataanalys.prediction.PredictionService;
import com.profession.suggest.database.services.pupil.PupilService;
import com.profession.suggest.dto.dataanalys.prediction.PredictionDTO;
import com.profession.suggest.dto.dataanalys.prediction.PredictionResponse;
import com.profession.suggest.dto.dataanalys.prediction.math.MathPredictionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.AccountNotFoundException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/predictions")
public class PredictionController {

    private final PredictionService predictionService;
    private final ClusterPredictionService clusterService;
    private final MathPredictionService mathService;
    private final AccountService accountService;
    private final PupilService pupilService;

    // ============ CLUSTER writes ============

    @HasRole(RoleEnum.PUPIL)
    @PostMapping("/predict")
    public ResponseEntity<PredictionResponse> predictCluster(
            @RequestAttribute("accountId") Long accountId) throws AccountNotFoundException {
        Prediction saved = predictionService.predictCluster(
                accountService.getAccountById(accountId));
        return ResponseEntity.ok(clusterService.toResponse(saved));
    }

    // ============ CLUSTER reads ============

    @HasRole(RoleEnum.PUPIL)
    @GetMapping("/latest")
    public ResponseEntity<PredictionResponse> getLatestCluster(
            @RequestAttribute("accountId") Long accountId) {
        return ResponseEntity.ok(clusterService.getLatestByAccountId(accountId));
    }
    // ============ MATH writes ============

    @HasRole(RoleEnum.PUPIL)
    @PostMapping("/math")
    public ResponseEntity<MathPredictionDTO> predictMath(
            @RequestAttribute("accountId") Long accountId) throws AccountNotFoundException {
        MathPrediction saved = predictionService.predictMath(
                accountService.getAccountById(accountId));
        return ResponseEntity.ok(mathService.toDTO(saved));
    }

    // ============ MATH reads ============

    @HasRole(RoleEnum.PUPIL)
    @GetMapping("/math/latest")
    public ResponseEntity<MathPredictionDTO> getLatestMath(
            @RequestAttribute("accountId") Long accountId) {
        return ResponseEntity.ok(mathService.getLatestByAccountId(accountId));
    }
    //TODO check this method
    @HasRole({RoleEnum.ADMIN, RoleEnum.CURATOR})
    @GetMapping("/pupil/{pupilId}")
    public ResponseEntity<List<PredictionDTO>> getClusterByPupilId(
            @RequestAttribute("accountId") Long accountId,
            @PathVariable("pupilId") Long pupilId) throws Exception {
        pupilService.getPupilDataForRequester(accountId, pupilId);
        return ResponseEntity.ok(clusterService.getByPupilId(pupilId));
    }

    @HasRole({RoleEnum.ADMIN, RoleEnum.CURATOR})
    @GetMapping("/math/pupil/{pupilId}")
    public ResponseEntity<List<MathPredictionDTO>> getMathByPupilId(
            @RequestAttribute("accountId") Long accountId,
            @PathVariable("pupilId") Long pupilId) throws Exception {
        pupilService.getPupilDataForRequester(accountId, pupilId);
        return ResponseEntity.ok(mathService.getByPupilId(pupilId));
    }
}