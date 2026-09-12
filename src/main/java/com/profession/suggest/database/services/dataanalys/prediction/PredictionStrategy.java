package com.profession.suggest.database.services.dataanalys.prediction;

import com.profession.suggest.database.entities.auth.Account;
import com.profession.suggest.database.entities.dataanalys.prediction.PredictionTypeEnum;
import com.profession.suggest.database.entities.users.pupil.Pupil;
import com.profession.suggest.dto.dataanalys.prediction.PredictionRequest;

public interface PredictionStrategy<R, P> {
    PredictionTypeEnum type();
    PredictionRequest buildRequest(Account account, Pupil pupil);
    R call(PredictionRequest request, String url);
    void validate(R response, Long expectedPupilId);
    P save(R response, Pupil pupil);
}
