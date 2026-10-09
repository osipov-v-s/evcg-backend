package com.profession.suggest.database.services.file;

import com.profession.suggest.database.entities.auth.Account;
import com.profession.suggest.database.entities.auth.role.Role;
import com.profession.suggest.database.entities.auth.role.RoleEnum;
import com.profession.suggest.database.entities.files.FileKind;
import com.profession.suggest.database.entities.files.StoredFile;
import com.profession.suggest.database.repositories.files.StoredFileRepository;
import com.profession.suggest.database.services.auth.AccountService;
import com.profession.suggest.services.files.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.security.auth.login.AccountNotFoundException;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class StoredFileService {
    private final StoredFileRepository repository;
    private final FileStorageService storage;
    private final AccountService accountService;
    @Transactional
    public StoredFile store(MultipartFile file, FileKind kind, Long ownerAccountId) throws IOException {
        String key = storage.saveFile(file, defaultSubfolder(kind), true);
        StoredFile sf = new StoredFile();
        sf.setStorageKey(key);
        sf.setOriginalName(file.getOriginalFilename());
        sf.setContentType(file.getContentType());
        sf.setKind(kind);
        sf.setSizeBytes(file.getSize());
        sf.setOwnerAccountId(ownerAccountId);
        return repository.save(sf);
    }
    public StoredFile getStoredFile(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + id));
    }
    public Resource open(StoredFile file) {
        return new FileSystemResource(storage.resolve(file.getStorageKey()));
    }
    @Transactional
    public void delete(StoredFile file) throws IOException {
        storage.deleteFile(file.getStorageKey());
        repository.delete(file);
    }
    public void assertCanRead(StoredFile file, Long accountId) throws AccountNotFoundException {
        Account account = accountService.getAccountById(accountId);
        if (account.getRoles().stream().map(Role::getName).anyMatch(r -> r == RoleEnum.ADMIN)) return;
        if (file.getOwnerAccountId() != null && file.getOwnerAccountId().equals(accountId)) return;
        throw new SecurityException("No access to this file");
    }
    private String defaultSubfolder(FileKind kind) {
        return switch (kind) {
            case SIMULATION -> "simulations";
            case COMPARISON_SESSION -> "comparison/sessions";
            case COMPARISON_SAMPLE_IMAGE -> "comparison/collections";
            case VR_TEST_ASSET -> "vr_tests_asset";
        };
    }
}
