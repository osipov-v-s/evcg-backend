package com.profession.suggest.database.entities.dataanalys.prediction.math;

import com.profession.suggest.database.entities.dataanalys.prediction.PredictionType;
import com.profession.suggest.database.entities.users.pupil.Pupil;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "math_prediction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MathPrediction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "percentage")
    private Float percentage;
    @Column(name = "recommendation")
    private String recommendation;
    @Column(name = "aizen_norm")
    private Float aizenNorm;
    @Column(name = "belbin_norm")
    private Float belbinNorm;
    @Column(name = "bennet_norm")
    private Float bennetNorm;
    @Column(name = "final_score")
    private Float finalScore;
    @Column(name = "utility")
    private Float utility;
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @ManyToOne
    @JoinColumn(name = "pupil_id", nullable = false)
    private Pupil pupil;
    @ManyToOne
    @JoinColumn(name = "prediction_type_id", nullable = true)
    private PredictionType predictionType;
    @PrePersist
    private void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
