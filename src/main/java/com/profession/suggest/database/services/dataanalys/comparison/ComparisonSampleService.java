package com.profession.suggest.database.services.dataanalys.comparison;

import com.profession.suggest.database.entities.dataanalys.comparison.ComparisonSample;
import com.profession.suggest.database.entities.files.StoredFile;
import com.profession.suggest.database.repositories.dataanalys.comparison.ComparisonSampleRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class ComparisonSampleService {
    private final ComparisonSampleRepository repository;

    public ComparisonSample save(ComparisonSample sample) {
        return repository.save(sample);
    }
    public List<ComparisonSample> findByCollectionId(Long collectionId) {
        return repository.findByCollectionId(collectionId);
    }
    public void deleteByCollectionId(Long collectionId) {
        repository.deleteByCollectionId(collectionId);
    }
    public void deleteAllByCollectionId(Long collectionId) {
        repository.bulkDeleteByCollectionId(collectionId);
    }
    public boolean existsByImageFileId(Long fileId) {
        return repository.existsByImageFileId(fileId);
    }
    public List<StoredFile> findFilesByCollection(Long collectionId) {
        return repository.findFilesByCollection(collectionId);
    }
}
