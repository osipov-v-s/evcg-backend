package com.profession.suggest.database.entities.dataanalys.simulation;

import com.profession.suggest.database.entities.files.StoredFile;
import com.profession.suggest.database.entities.professions.Profession;
import com.profession.suggest.database.entities.users.pupil.Pupil;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
/**
 * insert into profession (name) values ('Взрывник'),('Горный инженер'),('Горный мастер'),('Водитель белаза'),('Горноспасатель');
 * */
@Entity
@Table(name = "simulation")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Simulation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "file_path")
    private String filePath;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private StoredFile file;
    @Column(name = "start_sim")
    private LocalDateTime startSimulation;
    @Column(name = "end_sim")
    private LocalDateTime endSimulation;
    @Column(name = "description", nullable = true)
    private String description;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pupil_id")
    private Pupil pupil;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "simulation_type_id")
    private SimulationType simulationType;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profession_id")
    private Profession profession;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id")
    private Scenario scenario;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "simulation_data_source_id")
    private SimulationDataSource simulationDataSource;
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
