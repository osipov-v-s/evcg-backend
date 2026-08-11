package com.profession.suggest.database.services.dataanalys.prediction;

import com.profession.suggest.database.entities.auth.Account;
import com.profession.suggest.database.entities.dataanalys.prediction.Prediction;
import com.profession.suggest.database.entities.dataanalys.prediction.PredictionType;
import com.profession.suggest.database.entities.dataanalys.prediction.PredictionTypeEnum;
import com.profession.suggest.database.entities.professions.Profession;
import com.profession.suggest.database.entities.users.pupil.Pupil;
import com.profession.suggest.database.entities.users.specialist.Specialist;
import com.profession.suggest.database.repositories.dataanalys.prediction.PredictionRepository;
import com.profession.suggest.database.repositories.dataanalys.prediction.PredictionTypeRepository;
import com.profession.suggest.database.services.dataanalys.psychtests.PsychTestService;
import com.profession.suggest.database.services.profession.ProfessionService;
import com.profession.suggest.database.services.pupil.PupilService;
import com.profession.suggest.database.services.specialist.SpecialistService;
import com.profession.suggest.dto.dataanalys.prediction.PredictionDTO;
import com.profession.suggest.dto.dataanalys.prediction.PredictionMapper;
import com.profession.suggest.dto.dataanalys.prediction.PredictionRequest;
import com.profession.suggest.dto.dataanalys.prediction.PredictionResponse;
import com.profession.suggest.dto.dataanalys.psychtests.PsychTestDTO;
import com.profession.suggest.services.files.FileStorageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.*;

@Service
@RequiredArgsConstructor
public class PredictionService {
    private final PredictionRepository repository;
    private final PredictionMapper mapper;
    private final PredictionTypeService predictionTypeService;
    private final PupilService pupilService;
    private final PsychTestService psychTestService;
    private final ProfessionService professionService;
    private final SpecialistService specialistService;
    private final FileStorageService fileStorageService;
    private final RestTemplate restTemplate;
    @Value("${prediction.service.url}")
    private String predictUrl;
    //Deprecated method for saving file + data (from service broadcast)
    /** Deprecated
    public PredictionDTO createPrediction(PredictionDTO dto, MultipartFile file) throws Exception {
        if (dto == null)
            throw new IllegalArgumentException("Prediction DTO cannot be null");
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("File cannot be null or empty");
        if (dto.getPupilId() == null || dto.getPupilId() <= 0)
            throw new IllegalArgumentException("Valid pupil ID is required");
        if (dto.getPredictionType() == null)
            throw new IllegalArgumentException("Prediction type is required");
        if (file.getSize() > 10 * 1024 * 1024)
            throw new IllegalArgumentException("File size more then 10MB");

        PredictionType type = predictionTypeService.getByName(dto.getPredictionType());
        Prediction prediction = mapper.fromDTO(dto, type);
        Pupil pupil = pupilService.getPupilById(dto.getPupilId());

        if (pupil == null) throw new EntityNotFoundException(
                String.format("Pupil not found with id: %d", dto.getPupilId()));
        if (type == null) throw new EntityNotFoundException(
                String.format("Prediction type not found: %s", dto.getPredictionType()));

        prediction.setFilePath(fileStorageService.saveFile(file, "predictions", true));
        prediction.setPupil(pupil);
        return mapper.toDTO(repository.save(prediction));
    }
     */
    public List<PredictionDTO> getPredictionsByPupilId(Long pupilId) {
        List<Prediction> predictions = repository.findByPupilId(pupilId);
        return predictions.stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }
    public PredictionResponse getLatestPredictionByAccountId(Long accountId) {
        Pupil pupil = pupilService.getPupilByAccountId(accountId);
        Prediction prediction = repository.findTopByPupilIdOrderByCreatedAtDesc(pupil.getId())
                .orElseThrow(() -> new RuntimeException("No prediction found"));
        return PredictionResponse.builder()
                .pupilId(pupil.getId())
                .cluster(prediction.getCluster())
                .predictedProfession(prediction.getPredictedProfession().getName())
                .nearestSpecialistId(prediction.getNearestSpecialist().getId())
                .distance(prediction.getDistance())
                .confidenceCategory(prediction.getConfidenceCategory())
                .createdAt(prediction.getCreatedAt())
                .build();
    }
    /**TODO
     * - think how to get all actual psychTest for pupil and send thins to the servie
     * - fix accepted data from service in format like there
     * - 1. On react call prediciton/predict (via token)
     * - 2. Find by token pupil and send him in this method +
     * - 3. Make here request to python service and wait for PredictionResponse (send pupil with actual psychTests) +
     * - 4. Save result somewhere (think where to save it current prediction little bit not fit for fields may be add new fields)
     * - 5. After result is revieced and saved send it back to the user (after some delay on react he sees PredicitonResponse data but from db)
     * */
    private PredictionResponse getPredictionByPupilAccount(Account account) {
        PredictionRequest predictionRequest = new PredictionRequest();
        predictionRequest.setPsychTests(psychTestService.getAccountRecentTests(account));
        predictionRequest.setFullName(account.getPupil().getFullName());
        predictionRequest.setPupilId(account.getPupil().getId());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PredictionRequest> requestEntity = new HttpEntity<>(predictionRequest, headers);

        try {
            ResponseEntity<PredictionResponse> response = restTemplate.exchange(
                    predictUrl,
                    HttpMethod.POST,
                    requestEntity,
                    PredictionResponse.class
            );

            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Failed to call prediction service at: " + predictUrl, e);
        }
    }
    private Prediction createPrediction(PredictionResponse predictionResponse) {
        Pupil pupil = pupilService.getPupilById(predictionResponse.getPupilId());
        if (pupil == null) {
            throw new RuntimeException("Pupil not found with id: " + predictionResponse.getPupilId());
        }

        Profession profession = professionService.getProfessionByName(
                predictionResponse.getPredictedProfession());
        if (profession == null) {
            throw new RuntimeException("Profession not found: " + predictionResponse.getPredictedProfession());
        }

        Specialist specialist = specialistService.getSpecialistById(
                predictionResponse.getNearestSpecialistId());
        if (specialist == null) {
            throw new RuntimeException("Specialist not found with id: " + predictionResponse.getNearestSpecialistId());
        }
        //as default temp
        PredictionType predictionType = predictionTypeService.getByName(PredictionTypeEnum.CLUSTER);

        Prediction prediction = Prediction.builder()
                .pupil(pupil)
                .predictedProfession(profession)
                .nearestSpecialist(specialist)
                .predictionType(predictionType)
                .cluster(predictionResponse.getCluster())
                .distance(predictionResponse.getDistance())
                .confidenceCategory(predictionResponse.getConfidenceCategory())
                .build();
        return repository.save(prediction);
    }
    public PredictionResponse predictByAccount(Account account) {
        PredictionResponse response = getPredictionByPupilAccount(account);
        createPrediction(response);
        return response;
    }

}
