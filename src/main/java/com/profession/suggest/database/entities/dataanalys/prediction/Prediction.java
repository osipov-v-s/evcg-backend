package com.profession.suggest.database.entities.dataanalys.prediction;

import com.profession.suggest.database.entities.professions.Profession;
import com.profession.suggest.database.entities.users.pupil.Pupil;
import com.profession.suggest.database.entities.users.specialist.Specialist;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "prediction")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Prediction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "file_path")
    private String filePath;
    @Column(name = "cluster")
    private Integer cluster;
    @Column(name = "distance")
    private Double distance;
    @Column(name = "confidence_category")
    private String confidenceCategory;
    @ManyToOne
    @JoinColumn(name = "predicted_profession_id", nullable = true)
    private Profession predictedProfession;
    @ManyToOne
    @JoinColumn(name = "nearest_specialist_id", nullable = true)
    private Specialist nearestSpecialist;
    @ManyToOne
    @JoinColumn(name = "prediction_type_id", nullable = true)
    private PredictionType predictionType;
    @ManyToOne
    @JoinColumn(name = "pupil_id", nullable = false)
    private Pupil pupil;
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

}
