package com.profession.suggest.database.services.dataanalys.comparison;

import com.profession.suggest.database.entities.dataanalys.comparison.ComparisonCollection;
import com.profession.suggest.database.entities.dataanalys.comparison.ComparisonSample;
import com.profession.suggest.database.entities.files.FileKind;
import com.profession.suggest.database.entities.files.StoredFile;
import com.profession.suggest.database.repositories.dataanalys.comparison.ComparisonCollectionRepository;
import com.profession.suggest.database.services.file.StoredFileService;
import com.profession.suggest.dto.dataanalys.comparison.CollectionResponseDTO;
import com.profession.suggest.dto.dataanalys.comparison.CreateCollectionRequest;
import com.profession.suggest.dto.dataanalys.comparison.SampleDTO;
import com.profession.suggest.services.files.FileStorageService;
import com.profession.suggest.services.files.FileUrlService;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class ComparisonCollectionService {
    private final ComparisonCollectionRepository repository;
    private final ComparisonSampleService sampleService;
    private final FileStorageService fileStorageService;
    private final FileUrlService fileUrlService;
    private final StoredFileService storedFileService;
    private final EntityManager entityManager;
    /**
     * Get the most recent active collection with its samples
     * This is what the client requests before starting the test
     */
    public CollectionResponseDTO getMostRecentActiveCollection() {
        List<ComparisonCollection> collections = repository.findActiveCollectionsOrdered();
        if (collections.isEmpty()) throw new IllegalArgumentException("No active collections found");
        ComparisonCollection collection = collections.get(0);
        if (collection.getSamples() == null || collection.getSamples().isEmpty()) {
            collection = repository.findByIdWithSamples(collection.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Collection not found"));
        }
        return toResponseDTO(repository.findByIdWithSamples(collection.getId())
                .orElseThrow(() -> new IllegalArgumentException("Collection not found")));
    }
    /**
     * Get collection by ID with all samples
     */
    public CollectionResponseDTO getCollectionById(Long collectionId) {
        ComparisonCollection collection = repository.findByIdWithSamples(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found with id: " + collectionId));

        return toResponseDTO(collection);
    }
    /**
     * Get all active collections (for admin/selection purposes)
     */
    public List<CollectionResponseDTO> getAllActiveCollections() {
        List<ComparisonCollection> collections = repository.findActiveCollectionsOrdered();
        return collections.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }
    public Page<CollectionResponseDTO> getAllActiveCollections(Pageable pageable) {
        Page<ComparisonCollection> collections = repository.findActiveCollectionsOrdered(pageable);
        return collections.map(this::toResponseDTO);
    }
    @Transactional
    public CollectionResponseDTO createCollectionWithSamples(CreateCollectionRequest req) {
        log.info("Creating collection: {}", req.getName());
        if (req.getName() == null || req.getName().isBlank())
            throw new IllegalArgumentException("name is required");
        if (req.getSamples() == null || req.getSamples().isEmpty())
            throw new IllegalArgumentException("No samples found");
        ComparisonCollection collection = new ComparisonCollection();
        collection.setName(req.getName());
        collection.setDescription(req.getDescription());
        collection.setIsActive(true);
        collection.setAppVersion(req.getAppVersion() != null ? req.getAppVersion() : "0.1.0");
        collection.setDataSchemaVersion(req.getDataSchemaVersion() != null ? req.getDataSchemaVersion() : "1.0");
        ComparisonCollection savedCollection = repository.save(collection);
        log.info("Created collection with ID: {}", savedCollection.getId());

        List<ComparisonSample> savedSamples = new ArrayList<>();
        for (int i = 0; i < req.getSamples().size();i++) {
            CreateCollectionRequest.SampleItem item = req.getSamples().get(i);
            if (item.getFileId() == null)
                throw new IllegalArgumentException("samples[" + i + "].fieldId is required");
            if (item.getSampleName() == null || item.getSampleName().isBlank())
                item.setSampleName("SampleItem " + i);
            StoredFile stored = storedFileService.getStoredFile(item.getFileId());
            if (stored.getKind() != FileKind.COMPARISON_SAMPLE_IMAGE)
                throw new IllegalArgumentException("sample with index " + i + "must reference a COMPARISON_SAMPLE_IMAGE file" );
            ComparisonSample sample = new ComparisonSample();
            sample.setSampleName(item.getSampleName());
            sample.setImageFile(stored);
            sample.setDescription(item.getDescription());
            sample.setDisplayOrder(i);
            sample.setCollection(savedCollection);
            ComparisonSample savedSample = sampleService.save(sample);
            savedSamples.add(savedSample);
            log.info("Created sample: {} with file id: {}", item.getSampleName(), stored.getId());
        }
        savedCollection.setSamples(savedSamples);
        return toResponseDTO(savedCollection);
    }
    @Transactional
    public void deleteCollection(Long collectionId) throws IOException {
        if (!repository.existsById(collectionId))
            throw new IllegalArgumentException("Collection not found");
        List<StoredFile> files = sampleService.findFilesByCollection(collectionId);

        sampleService.deleteAllByCollectionId(collectionId);
        repository.deleteById(collectionId);
        for (StoredFile f: files){
            if (sampleService.existsByImageFileId(f.getId())) continue;
            storedFileService.delete(f);
        }
        log.info("Deleted collection and all samples: {}", collectionId);
    }
    public Optional<ComparisonCollection> findById(Long collectionId) {
        return repository.findById(collectionId);
    }

    /**
     * Convert entity to DTO
     */
    private CollectionResponseDTO toResponseDTO(ComparisonCollection collection) {
        List<SampleDTO> sampleDTOs = collection.getSamples().stream()
                .map(sample -> SampleDTO.builder()
                        .id(sample.getId())
                        .sampleName(sample.getSampleName())
                        .file(fileUrlService.toDto(sample.getImageFile()))
                        .description(sample.getDescription())
                        .displayOrder(sample.getDisplayOrder())
                        .build())
                .collect(Collectors.toList());

        return CollectionResponseDTO.builder()
                .id(collection.getId())
                .name(collection.getName())
                .description(collection.getDescription())
                .isActive(collection.getIsActive())
                .appVersion(collection.getAppVersion())
                .dataSchemaVersion(collection.getDataSchemaVersion())
                .samples(sampleDTOs)
                .build();
    }

}
