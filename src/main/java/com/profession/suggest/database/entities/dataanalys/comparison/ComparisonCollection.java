package com.profession.suggest.database.entities.dataanalys.comparison;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comparison_collection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonCollection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "app_version")
    private String appVersion = "0.1.0";

    @Column(name = "data_schema_version")
    private String dataSchemaVersion = "1.0";

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
    @OneToMany(mappedBy = "collection")
    private List<ComparisonSample> samples = new ArrayList<>();
}
