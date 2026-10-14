package com.profession.suggest.database.repositories.dataanalys.comparison;

import com.profession.suggest.database.entities.dataanalys.comparison.ComparisonSample;
import com.profession.suggest.database.entities.files.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ComparisonSampleRepository extends JpaRepository<ComparisonSample, Long> {
    List<ComparisonSample> findByCollectionId(Long collectionId);
    void deleteByCollectionId(Long collectionId);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ComparisonSample s WHERE s.collection.id = :collectionId")
    void bulkDeleteByCollectionId(@Param("collectionId") Long collectionId);
    boolean existsByImageFileId(Long imageFileId);
    @Query("SELECT s.imageFile FROM ComparisonSample s WHERE s.collection.id = :collectionId")
    List<StoredFile> findFilesByCollection(@Param("collectionId") Long collectionId);
}
