package com.profession.suggest.database.services.dataanalys.prediction;

import com.profession.suggest.database.entities.auth.Account;
import com.profession.suggest.database.entities.dataanalys.prediction.Prediction;
import com.profession.suggest.database.entities.dataanalys.prediction.PredictionType;
import com.profession.suggest.database.entities.professions.Profession;
import com.profession.suggest.database.entities.users.pupil.Pupil;
import com.profession.suggest.database.entities.users.specialist.Specialist;
import com.profession.suggest.database.repositories.dataanalys.prediction.PredictionRepository;
import com.profession.suggest.database.services.dataanalys.psychtests.PsychTestService;
import com.profession.suggest.database.services.profession.ProfessionService;
import com.profession.suggest.database.services.pupil.PupilService;
import com.profession.suggest.database.services.specialist.SpecialistService;
import com.profession.suggest.dto.dataanalys.prediction.PredictionMapper;
import com.profession.suggest.dto.dataanalys.prediction.PredictionRequest;
import com.profession.suggest.dto.dataanalys.prediction.PredictionResponse;
import com.profession.suggest.exceptions.PredictionIntegrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PredictionServiceTest {
    @Mock private PredictionRepository repository;
    @Mock private PredictionMapper mapper;
    @Mock private PredictionTypeService predictionTypeService;
    @Mock private PupilService pupilService;
    @Mock private PsychTestService psychTestService;
    @Mock private ProfessionService professionService;
    @Mock private SpecialistService specialistService;
    @Mock private RestTemplate restTemplate;

    private PredictionService service;
    private Account account;
    private Pupil pupil;

    @BeforeEach
    void setUp() {
        service = new PredictionService(
                repository,
                mapper,
                predictionTypeService,
                pupilService,
                psychTestService,
                professionService,
                specialistService,
                restTemplate);
        ReflectionTestUtils.setField(service, "predictUrl", "http://prediction.test/predict");
        pupil = new Pupil();
        pupil.setId(17L);
        account = new Account();
        account.setId(3L);
        account.setPupil(pupil);
        when(psychTestService.getAccountRecentTests(account)).thenReturn(new LinkedHashMap<>());
    }

    @Test
    void validPythonResponseIsValidatedStoredAndMappedFromSavedEntity() {
        PredictionResponse external = responseFor(17L);
        when(restTemplate.exchange(
                eq("http://prediction.test/predict"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(PredictionResponse.class)))
                .thenReturn(ResponseEntity.ok(external));
        Profession profession = new Profession();
        profession.setName("Profession A");
        Specialist specialist = new Specialist();
        specialist.setId(41L);
        PredictionType type = new PredictionType();
        when(professionService.getProfessionByName("Profession A")).thenReturn(profession);
        when(specialistService.getSpecialistById(41L)).thenReturn(specialist);
        when(predictionTypeService.getByName(any())).thenReturn(type);
        LocalDateTime savedAt = LocalDateTime.of(2026, 8, 12, 10, 30);
        when(repository.save(any(Prediction.class))).thenAnswer(invocation -> {
            Prediction saved = invocation.getArgument(0);
            saved.setId(100L);
            saved.setCreatedAt(savedAt);
            return saved;
        });

        PredictionResponse result = service.predictByAccount(account);

        assertEquals(savedAt, result.getCreatedAt());
        assertEquals(17L, result.getPupilId());
        assertEquals("Profession A", result.getPredictedProfession());
        ArgumentCaptor<Prediction> savedPrediction = ArgumentCaptor.forClass(Prediction.class);
        verify(repository).save(savedPrediction.capture());
        assertSame(pupil, savedPrediction.getValue().getPupil());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<PredictionRequest>> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq("http://prediction.test/predict"),
                eq(HttpMethod.POST),
                requestCaptor.capture(),
                eq(PredictionResponse.class));
        assertEquals(17L, requestCaptor.getValue().getBody().getPupilId());
    }

    @Test
    void timeoutBecomesControlledGatewayTimeout() {
        when(restTemplate.exchange(any(String.class), any(), any(), eq(PredictionResponse.class)))
                .thenThrow(new ResourceAccessException("timeout", new SocketTimeoutException("read timed out")));

        PredictionIntegrationException exception = assertThrows(
                PredictionIntegrationException.class,
                () -> service.predictByAccount(account));

        assertEquals("PREDICTION_TIMEOUT", exception.getCode());
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, exception.getStatus());
        verify(repository, never()).save(any());
    }

    @Test
    void pythonServerErrorBecomesControlledServiceUnavailable() {
        when(restTemplate.exchange(any(String.class), any(), any(), eq(PredictionResponse.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        PredictionIntegrationException exception = assertThrows(
                PredictionIntegrationException.class,
                () -> service.predictByAccount(account));

        assertEquals("PREDICTION_SERVICE_ERROR", exception.getCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        verify(repository, never()).save(any());
    }

    @Test
    void responseForAnotherPupilIsRejectedAndNotStored() {
        when(restTemplate.exchange(any(String.class), any(), any(), eq(PredictionResponse.class)))
                .thenReturn(ResponseEntity.ok(responseFor(999L)));

        PredictionIntegrationException exception = assertThrows(
                PredictionIntegrationException.class,
                () -> service.predictByAccount(account));

        assertEquals("PREDICTION_INVALID_RESPONSE", exception.getCode());
        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        verify(repository, never()).save(any());
    }

    private PredictionResponse responseFor(long pupilId) {
        return PredictionResponse.builder()
                .pupilId(pupilId)
                .cluster(1)
                .predictedProfession("Profession A")
                .nearestSpecialistId(41L)
                .distance(0.75)
                .confidenceCategory("LOW")
                .build();
    }
}
