package com.profession.suggest.database.entities.dataanalys.comparison;

import com.profession.suggest.database.entities.auth.Account;
import com.profession.suggest.database.entities.files.StoredFile;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "comparison_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_session_id", nullable = false, unique = true)
    private String clientSessionId;

    @Column(name = "file_path", nullable = false)
    private String filePath;  // Path to the uploaded JSON file
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private StoredFile file;
    // Metadata from the JSON
    @Column(name = "app_version")
    private String appVersion;

    @Column(name = "data_schema_version")
    private String dataSchemaVersion;

    @Column(name = "data_source_mode")
    private String dataSourceMode;  // "mock", "real", etc.

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "total_duration_seconds")
    private Double totalDurationSeconds;

    @Column(name = "is_completed")
    private Boolean isCompleted = false;

    @Column(name = "eye_tracking_available")
    private Boolean eyeTrackingAvailable = false;

    @Column(name = "face_tracking_available")
    private Boolean faceTrackingAvailable = false;

    @Column(name = "upload_status")
    private String uploadStatus;  // "pending", "uploaded", "processed", "failed"

    @Column(name = "record_count")
    private Integer recordCount;  // How many comparisons in the file

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;  // Optional: track file size

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", nullable = false)
    private ComparisonCollection collection;
}
