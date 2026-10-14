package com.profession.suggest.database.services.dataanalys.comparison;

import com.profession.suggest.database.entities.auth.Account;
import com.profession.suggest.database.entities.auth.role.Role;
import com.profession.suggest.database.entities.auth.role.RoleEnum;
import com.profession.suggest.database.entities.dataanalys.comparison.ComparisonCollection;
import com.profession.suggest.database.entities.dataanalys.comparison.ComparisonSession;
import com.profession.suggest.database.entities.files.FileKind;
import com.profession.suggest.database.entities.files.StoredFile;
import com.profession.suggest.database.repositories.dataanalys.comparison.ComparisonSessionRepository;
import com.profession.suggest.database.services.auth.AccountService;
import com.profession.suggest.database.services.file.StoredFileService;
import com.profession.suggest.dto.dataanalys.comparison.SessionData;
import com.profession.suggest.dto.dataanalys.comparison.SessionResponseDTO;
import com.profession.suggest.services.files.FileStorageService;
import com.profession.suggest.services.files.FileUrlService;
import com.profession.suggest.services.json.JsonParserService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.security.auth.login.AccountNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
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
    private final AccountService accountService;

    @Transactional
    public SessionResponseDTO submitSessionFromFileId(Long accountId, Long fileId) throws AccountNotFoundException, IOException {
        if (fileId == null)
            throw new IllegalArgumentException("fileId is required");
        Account account = accountService.getAccountById(accountId);
        StoredFile stored = storedFileService.getStoredFile(fileId);
        if (stored.getKind() != FileKind.COMPARISON_SESSION)
            throw new IllegalArgumentException("fileId must references a COMPARISON_SESSION file");
        boolean isAdmin = account.getRoles().stream()
                .map(Role::getName)
                .anyMatch(r-> r == RoleEnum.ADMIN);
        if (!isAdmin && !Objects.equals(stored.getOwnerAccountId(), account.getId()))
            throw new SecurityException("fileId does not belong to requester");
        SessionData sessionData = jsonParserService.parseJsonToClass(
                storedFileService.open(stored), SessionData.class
        );
        return persistSession(account, sessionData, stored);
    }
    private SessionResponseDTO persistSession(Account account, SessionData sessionData, StoredFile stored) {
        if (sessionData.getCollectionId() == null)
            throw new IllegalArgumentException("collectionId is required");
        if (sessionData.getClientSessionId() != null && repository.findByClientSessionId(sessionData.getClientSessionId()).isPresent())
            throw new IllegalArgumentException("Session already exists: " + sessionData.getClientSessionId());
        ComparisonCollection collection = collectionService.findById(sessionData.getCollectionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Collection not found: " + sessionData.getCollectionId()
                ));
        ComparisonSession session = new ComparisonSession();
        session.setClientSessionId(sessionData.getClientSessionId());
        session.setFile(stored);
        session.setAppVersion(sessionData.getAppVersion());
        session.setDataSchemaVersion(sessionData.getDataSchemaVersion());
        session.setDataSourceMode(sessionData.getDataSourceMode());
        session.setStartedAt(sessionData.getStartedAt());
        session.setCompletedAt(sessionData.getCompletedAt());
        session.setTotalDurationSeconds(sessionData.getTotalDurationSeconds());
        session.setIsCompleted(sessionData.getIsCompleted());
        session.setEyeTrackingAvailable(sessionData.getEyeTrackingAvailable());
        session.setFaceTrackingAvailable(sessionData.getFaceTrackingAvailable());
        session.setUploadStatus(session.getUploadStatus() != null ? session.getUploadStatus() : "uploaded");
        session.setRecordCount(sessionData.getRecordCount());
        session.setFileSizeBytes(stored.getSizeBytes());
        session.setAccount(account);
        session.setCollection(collection);

        ComparisonSession saved = repository.save(session);
        return toResponseDTO(saved);

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
        if (stored != null)
            storedFileService.delete(session.getFile());

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
