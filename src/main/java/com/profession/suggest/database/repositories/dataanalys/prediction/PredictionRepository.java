package com.profession.suggest.database.repositories.dataanalys.prediction;

import com.profession.suggest.database.entities.dataanalys.prediction.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {
    List<Prediction> findByPupilId(Long pupilId);
    Optional<Prediction> findTopByPupilIdOrderByCreatedAtDesc(Long pupilId);
}
