package com.profession.suggest.database.entities.dataanalys.comparison;

import com.profession.suggest.database.entities.files.StoredFile;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "comparison_sample")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonSample {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sample_name", nullable = false)
    private String sampleName;

    @Column(name = "image_path", nullable = false)
    private String imagePath;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_file_id")
    private StoredFile imageFile;

    @Column(name = "description")
    private String description;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", nullable = false)
    private ComparisonCollection collection;
}
