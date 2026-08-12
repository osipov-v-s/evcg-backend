package com.profession.suggest.controllers.comparison;

import com.profession.suggest.configuration.security.annotation.HasRole;
import com.profession.suggest.database.entities.auth.Account;
import com.profession.suggest.database.entities.auth.role.RoleEnum;
import com.profession.suggest.database.entities.dataanalys.comparison.ComparisonCollection;
import com.profession.suggest.database.entities.dataanalys.comparison.ComparisonSession;
import com.profession.suggest.database.services.auth.AccountService;
import com.profession.suggest.database.services.dataanalys.comparison.ComparisonCollectionService;
import com.profession.suggest.database.services.dataanalys.comparison.ComparisonSessionService;
import com.profession.suggest.dto.dataanalys.comparison.CollectionResponseDTO;
import com.profession.suggest.dto.dataanalys.comparison.SessionData;
import com.profession.suggest.dto.dataanalys.comparison.SessionResponseDTO;
import com.profession.suggest.dto.dataanalys.comparison.SessionSubmitRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/comparison")
@AllArgsConstructor
@Slf4j
public class ComparisonController {
    private final ComparisonCollectionService collectionService;
    private final ComparisonSessionService sessionService;
    private final AccountService accountService;

    /**
     * Client gets the most recent active collection to start the test
     * GET /api/comparison/collections/active
     */
    @HasRole({RoleEnum.PUPIL, RoleEnum.SPECIALIST})
    @GetMapping("/collections/active")
    public ResponseEntity<?> getActiveCollection() {
        try {
            CollectionResponseDTO collection = collectionService.getMostRecentActiveCollection();
            return ResponseEntity.ok(collection);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        } catch (Exception e) {
            log.error("Error getting active collection: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to get active collection");
        }
    }
    /**
     * Client submits completed test session
     * POST /api/comparison/sessions
     */
    @HasRole({RoleEnum.PUPIL, RoleEnum.SPECIALIST})
    @PostMapping("/sessions")
    public ResponseEntity<?> submitSession(
            @RequestAttribute("accountId") Long accountId,
            @RequestParam MultipartFile dataFile) {
        try {
            Account account = accountService.getAccountById(accountId);
            SessionResponseDTO session = sessionService.submitSession(account, dataFile);
            return ResponseEntity.status(HttpStatus.CREATED).body(session);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (IOException e) {
            log.error("File save error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to save session file");
        } catch (Exception e) {
            log.error("Error submitting session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to submit session");
        }
    }
    /**
     * Client gets their sessions (paginated)
     * GET /api/comparison/sessions
     */
    @HasRole({RoleEnum.PUPIL, RoleEnum.SPECIALIST})
    @GetMapping("/sessions")
    public ResponseEntity<?> getMySessions(
            @RequestAttribute("accountId") Long accountId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        try {
            Account account = accountService.getAccountById(accountId);
            Page<SessionResponseDTO> sessions = sessionService.getSessionsByAccount(account, pageable);
            return ResponseEntity.ok(sessions);
        } catch (Exception e) {
            log.error("Error getting sessions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to get sessions");
        }
    }
    /**
     * Client gets a specific session by ID
     * GET /api/comparison/sessions/{sessionId}
     */
    @HasRole({RoleEnum.PUPIL, RoleEnum.SPECIALIST})
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<?> getSession(
            @RequestAttribute("accountId") Long accountId,
            @PathVariable Long sessionId) {
        try {
            Account account = accountService.getAccountById(accountId);
            SessionResponseDTO session = sessionService.toResponseDTO(sessionService.getSessionById(sessionId, account));
            return ResponseEntity.ok(session);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());
        } catch (Exception e) {
            log.error("Error getting session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to get session");
        }
    }
    /**
     * Client deletes their session
     * DELETE /api/comparison/sessions/{sessionId}
     */
    @HasRole({RoleEnum.PUPIL, RoleEnum.SPECIALIST})
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<?> deleteSession(
            @RequestAttribute("accountId") Long accountId,
            @PathVariable Long sessionId) {
        try {
            Account account = accountService.getAccountById(accountId);
            sessionService.deleteSession(sessionId, account);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());
        } catch (IOException e) {
            log.error("File delete error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete session file");
        } catch (Exception e) {
            log.error("Error deleting session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete session");
        }
    }
    // ADMIN ENDPOINTS
    /**
     * Admin gets all active collections with pagination
     * GET /api/comparison/collections?page=0&size=10&sort=createdAt,desc
     */
    @HasRole(RoleEnum.ADMIN)
    @GetMapping("/collections")
    public ResponseEntity<?> getAllCollections(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        try {
            Page<CollectionResponseDTO> collections = collectionService.getAllActiveCollections(pageable);
            return ResponseEntity.ok(collections);
        } catch (Exception e) {
            log.error("Error getting collections: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to get collections");
        }
    }
    /**
     * Admin creates a new collection with samples
     * POST /api/comparison/collections
     */
    @HasRole(RoleEnum.ADMIN)
    @PostMapping("/collections")
    public ResponseEntity<?> createCollection(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam("images") List<MultipartFile> images,
            @RequestParam("sampleNames") List<String> sampleNames,
            @RequestParam(required = false) List<String> sampleDescriptions,
            @RequestParam(required = false) String appVersion,
            @RequestParam(required = false) String dataSchemaVersion) {
        try {
            CollectionResponseDTO collection = collectionService.createCollectionWithSamples(
                    name, description, images, sampleNames, sampleDescriptions,
                    appVersion, dataSchemaVersion);
            return ResponseEntity.status(HttpStatus.CREATED).body(collection);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (IOException e) {
            log.error("File save error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to save images");
        } catch (Exception e) {
            log.error("Error creating collection: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create collection");
        }
    }
    /**
     * Admin deletes a collection (and all samples/images) but only if no sessions
     * DELETE /api/comparison/collections/{collectionId}
     */
    @HasRole(RoleEnum.ADMIN)
    @DeleteMapping("/collections/{collectionId}")
    public ResponseEntity<?> deleteCollection(@PathVariable Long collectionId) {
        try {
            List<SessionResponseDTO> sessions = sessionService.getSessionsByCollection(collectionId);
            if (!sessions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Cannot delete collection. It has " + sessions.size()
                                + " completed session(s). Delete sessions first.");
            }
            collectionService.deleteCollection(collectionId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        } catch (IOException e) {
            log.error("File delete error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete collection images");
        } catch (Exception e) {
            log.error("Error deleting collection: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete collection");
        }
    }
    /**
     * Admin gets all sessions for a collection
     * GET /api/comparison/collections/{collectionId}/sessions
     */
    @HasRole(RoleEnum.ADMIN)
    @GetMapping("/collections/{collectionId}/sessions")
    public ResponseEntity<?> getSessionsByCollection(@PathVariable Long collectionId) {
        try {
            List<SessionResponseDTO> sessions = sessionService.getSessionsByCollection(collectionId);
            return ResponseEntity.ok(sessions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        } catch (Exception e) {
            log.error("Error getting sessions by collection: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to get sessions");
        }
    }
}
