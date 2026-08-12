package com.profession.suggest.database.entities.dataanalys.psychtests;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "psych_test_type")
public class PsychTestType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name", unique = true, nullable = false)
    private String name;
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    @OneToMany(mappedBy = "psychTestType", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PsychTest> psychTests;
}
