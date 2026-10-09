package com.profession.suggest.database.repositories.files;

import com.profession.suggest.database.entities.files.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
    Optional<StoredFile> findByStorageKey(String storageKey);
}
