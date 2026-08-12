package com.profession.suggest.database.entities.dataanalys.vrtests;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "vr_test_type")
public class VRTestType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name", unique = true, nullable = false)
    private String name;
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
