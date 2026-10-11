package com.profession.suggest.database.services.dataanalys.comparison;

import com.profession.suggest.database.entities.dataanalys.comparison.ComparisonCollection;
import com.profession.suggest.database.entities.dataanalys.comparison.ComparisonSample;
import com.profession.suggest.database.entities.files.FileKind;
import com.profession.suggest.database.entities.files.StoredFile;
import com.profession.suggest.database.repositories.dataanalys.comparison.ComparisonCollectionRepository;
import com.profession.suggest.database.services.file.StoredFileService;
import com.profession.suggest.dto.dataanalys.comparison.CollectionResponseDTO;
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
    public CollectionResponseDTO createCollectionWithSamples(
            String name,
            String description,
            List<MultipartFile> images,
            List<String> sampleNames,
            List<String> sampleDescriptions,
            String appVersion,
            String dataSchemaVersion) throws IOException {

        log.info("Creating collection: {}", name);

        // 1. Validate
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("At least one image is required");
        }
        if (sampleNames == null || sampleNames.size() != images.size()) {
            throw new IllegalArgumentException("Sample names count must match images count");
        }

        // 2. Create Collection
        ComparisonCollection collection = new ComparisonCollection();
        collection.setName(name);
        collection.setDescription(description);
        collection.setIsActive(true);
        collection.setAppVersion(appVersion != null ? appVersion : "0.1.0");
        collection.setDataSchemaVersion(dataSchemaVersion != null ? dataSchemaVersion : "1.0");

        ComparisonCollection savedCollection = repository.save(collection);
        log.info("Created collection with ID: {}", savedCollection.getId());

        // 3. Save images and create samples
        String subfolder = "comparison/collections/" + savedCollection.getId();
        List<ComparisonSample> savedSamples = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            MultipartFile image = images.get(i);
            String sampleName = sampleNames.get(i);
            String sampleDescription = sampleDescriptions != null && i < sampleDescriptions.size()
                    ? sampleDescriptions.get(i) : null;

            // Save image
            StoredFile storedFile = storedFileService.store(image, FileKind.COMPARISON_SAMPLE_IMAGE, null);
            //String imagePath = fileStorageService.saveFile(image, subfolder, true);
            //log.info("Saved image: {}", imagePath);

            // Create sample
            ComparisonSample sample = new ComparisonSample();
            sample.setSampleName(sampleName);
            sample.setImagePath(storedFile.getStorageKey());
            sample.setImageFile(storedFile);
            sample.setDescription(sampleDescription);
            sample.setDisplayOrder(i);
            sample.setCollection(savedCollection);

            ComparisonSample savedSample = sampleService.save(sample);
            savedSamples.add(savedSample);
            //log.info("Created sample: {} with image: {}", sampleName, imagePath);
        }
        savedCollection.setSamples(savedSamples);
        return toResponseDTO(savedCollection);
    }
    @Transactional
    public void deleteCollection(Long collectionId) throws IOException {
        ComparisonCollection collection = repository.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));

        // Delete all samples (images will be deleted too)
        List<ComparisonSample> samples = sampleService.findByCollectionId(collectionId);

        for (ComparisonSample sample : samples) {
            // Delete image file
            String imagePath = sample.getImagePath();
            StoredFile stored = sample.getImageFile();
            if (stored !=  null)
                storedFileService.delete(stored);
            if (imagePath != null && (stored == null || !imagePath.equals(stored.getStorageKey()))) {
                fileStorageService.deleteFile(imagePath);
            }
        }

        // Delete samples and collection
        sampleService.deleteByCollectionId(collectionId);
        repository.delete(collection);

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
                        .imagePath(sample.getImagePath())
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
