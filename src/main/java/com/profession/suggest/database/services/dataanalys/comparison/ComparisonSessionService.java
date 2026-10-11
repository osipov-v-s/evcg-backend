package com.profession.suggest.database.services.dataanalys.comparison;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profession.suggest.database.entities.auth.Account;
import com.profession.suggest.database.entities.dataanalys.comparison.ComparisonCollection;
import com.profession.suggest.database.entities.dataanalys.comparison.ComparisonSession;
import com.profession.suggest.database.entities.files.FileKind;
import com.profession.suggest.database.entities.files.StoredFile;
import com.profession.suggest.database.repositories.dataanalys.comparison.ComparisonSessionRepository;
import com.profession.suggest.database.services.file.StoredFileService;
import com.profession.suggest.dto.dataanalys.comparison.SessionData;
import com.profession.suggest.dto.dataanalys.comparison.SessionResponseDTO;
import com.profession.suggest.dto.dataanalys.comparison.SessionSubmitRequest;
import com.profession.suggest.services.files.FileStorageService;
import com.profession.suggest.services.files.FileUrlService;
import com.profession.suggest.services.json.JsonParserService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class ComparisonSessionService {
    private final ComparisonSessionRepository repository;
    private final ComparisonCollectionService collectionService;
    private final FileStorageService fileStorageService;
    private final StoredFileService storedFileService;
    private final FileUrlService fileUrlService;
    private final JsonParserService jsonParserService;


    /**
     * Submit a completed comparison test session
     * Saves the JSON file and creates a session record
     */
    @Transactional
    public SessionResponseDTO submitSession(Account account, MultipartFile dataFile) throws IOException {
        log.info("Submitting comparison session for account: {}", account.getId());
        SessionData sessionData = jsonParserService.parseJsonToClass(dataFile, SessionData.class);
        // 1. Validate
        if (sessionData.getClientSessionId() == null || sessionData.getClientSessionId().isEmpty()) {
            throw new IllegalArgumentException("clientSessionId is required");
        }
        if (sessionData.getCollectionId() == null) {
            throw new IllegalArgumentException("collectionId is required");
        }

        // Check if session already exists
        if (repository.findByClientSessionId(sessionData.getClientSessionId()).isPresent()) {
            throw new IllegalArgumentException("Session already exists: " + sessionData.getClientSessionId());
        }

        // 2. Get collection
        ComparisonCollection collection = collectionService.findById(sessionData.getCollectionId())
                .orElseThrow(() -> new IllegalArgumentException("Collection not found: " + sessionData.getCollectionId()));

        String subfolder = "comparison/sessions";
        //String filePath = fileStorageService.saveFile(dataFile, subfolder, true);
        StoredFile storedFile = storedFileService.store(dataFile, FileKind.COMPARISON_SESSION, account.getId());

        // 4. Create session record
        ComparisonSession session = new ComparisonSession();
        session.setClientSessionId(sessionData.getClientSessionId());
        session.setFilePath(storedFile.getStorageKey());
        session.setFile(storedFile);
        session.setAppVersion(sessionData.getAppVersion());
        session.setDataSchemaVersion(sessionData.getDataSchemaVersion());
        session.setDataSourceMode(sessionData.getDataSourceMode());
        session.setStartedAt(sessionData.getStartedAt());
        session.setCompletedAt(sessionData.getCompletedAt());
        session.setTotalDurationSeconds(sessionData.getTotalDurationSeconds());
        session.setIsCompleted(sessionData.getIsCompleted());
        session.setEyeTrackingAvailable(sessionData.getEyeTrackingAvailable());
        session.setFaceTrackingAvailable(sessionData.getFaceTrackingAvailable());
        session.setUploadStatus(sessionData.getUploadStatus() != null ? sessionData.getUploadStatus() : "uploaded");
        session.setRecordCount(sessionData.getRecordCount());
        session.setFileSizeBytes(dataFile.getSize());
        session.setAccount(account);
        session.setCollection(collection);

        ComparisonSession savedSession = repository.save(session);
        log.info("Saved session with ID: {}", savedSession.getId());

        return toResponseDTO(savedSession);
    }
    /**
     * Get sessions for an account with pagination
     */
    public Page<SessionResponseDTO> getSessionsByAccount(Account account, Pageable pageable) {
        Page<ComparisonSession> sessions = repository.findByAccountId(account.getId(), pageable);
        return sessions.map(this::toResponseDTO);
    }
    public List<SessionResponseDTO> getSessionByAccount(Account account) {
        List<ComparisonSession> sessions = repository.findByAccountId(account.getId());
        return sessions.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }
    /**
     * Get session by ID (with ownership check)
     */
    public ComparisonSession getSessionById(Long sessionId, Account account) {
        ComparisonSession session = repository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        if (!session.getAccount().getId().equals(account.getId())) {
            throw new SecurityException("You don't have access to this session");
        }

        return session;
    }
    /**
     * Count sessions for a user and collection
     */
    public long countSessionsByAccountAndCollection(Account account, Long collectionId) {
        return repository.countByAccountIdAndCollectionId(account.getId(), collectionId);
    }
    /**
     * Delete a session (and its file)
     */
    @Transactional
    public void deleteSession(Long sessionId, Account account) throws IOException {
        ComparisonSession session = getSessionById(sessionId, account);
        StoredFile stored = session.getFile();
        String filePath = session.getFilePath();
        if (stored != null)
            storedFileService.delete(session.getFile());
        if (filePath != null && (stored == null || !filePath.equals(stored.getStorageKey())))
            fileStorageService.deleteFile(filePath);

        repository.delete(session);
    }

    /**
     * Get sessions for a specific collection
     */
    public List<SessionResponseDTO> getSessionsByCollection(Long collectionId) {
        List<ComparisonSession> sessions = repository.findByCollectionId(collectionId);
        return sessions.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convert entity to DTO
     */
    public SessionResponseDTO toResponseDTO(ComparisonSession session) {
        return SessionResponseDTO.builder()
                .id(session.getId())
                .clientSessionId(session.getClientSessionId())
                .collectionId(session.getCollection().getId())
                .collectionName(session.getCollection().getName())
                .filePath(session.getFilePath())
                .file(fileUrlService.toDto(session.getFile()))
                .recordCount(session.getRecordCount())
                .startedAt(session.getStartedAt())
                .completedAt(session.getCompletedAt())
                .totalDurationSeconds(session.getTotalDurationSeconds())
                .isCompleted(session.getIsCompleted())
                .uploadStatus(session.getUploadStatus())
                .createdAt(session.getCreatedAt())
                .build();
    }

}
