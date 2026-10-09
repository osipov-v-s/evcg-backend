package com.profession.suggest.database.entities.files;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "stored_file")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StoredFile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    private String storageKey;
    @Column(name = "original_name", length = 500)
    private String originalName;
    @Column(name = "content_type", length = 120)
    private String contentType;
    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 40)
    private FileKind kind;
    @Column(name = "owner_account_id")
    private Long ownerAccountId;
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
