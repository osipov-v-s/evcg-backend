package com.profession.suggest.database.entities.professions;

import com.profession.suggest.database.entities.dataanalys.prediction.Prediction;
import com.profession.suggest.database.entities.dataanalys.simulation.Simulation;
import com.profession.suggest.database.entities.dataanalys.vrtests.VRTest;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "profession")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Profession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name", unique = true)
    private String name;
    @OneToMany(mappedBy = "profession")
    private List<Simulation> simulations;
    @OneToMany(mappedBy = "profession")
    private List<VRTest> vrTests;
    @OneToMany(mappedBy = "predictedProfession")
    private List<Prediction> predictions;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profession_sphere_id")
    private ProfessionSphere professionSphere;
}
