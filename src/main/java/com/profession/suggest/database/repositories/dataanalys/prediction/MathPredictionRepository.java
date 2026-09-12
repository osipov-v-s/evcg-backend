package com.profession.suggest.database.repositories.dataanalys.prediction;

import com.profession.suggest.database.entities.dataanalys.prediction.math.MathPrediction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MathPredictionRepository extends JpaRepository<MathPrediction, Long> {
    List<MathPrediction> findByPupilId(Long pupilId);
    Optional<MathPrediction> findTopByPupilIdOrderByCreatedAtDesc(Long pupilId);

}
