package com.profession.suggest.controllers.files;

import com.profession.suggest.configuration.security.annotation.HasRole;
import com.profession.suggest.database.entities.auth.role.RoleEnum;
import com.profession.suggest.database.entities.files.FileKind;
import com.profession.suggest.database.entities.files.StoredFile;
import com.profession.suggest.database.services.auth.AccountService;
import com.profession.suggest.database.services.file.StoredFileService;
import com.profession.suggest.dto.file.StoredFileDTO;
import com.profession.suggest.services.files.FileUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.security.auth.login.AccountNotFoundException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
    private final StoredFileService storedFileService;
    private final FileUrlService fileUrlService;
    private final AccountService accountService;
    // Download (existing files via api)
    @GetMapping("/{fileId}/content")
    public ResponseEntity<Resource> content(@PathVariable Long fileId, @RequestAttribute("accountId") Long accountId) throws AccountNotFoundException {
        StoredFile f = storedFileService.getStoredFile(fileId);
        storedFileService.assertCanRead(f, accountId);
        MediaType type = f.getContentType() != null
                ? MediaType.parseMediaType(f.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(storedFileService.open(f));
    }
    // Upload new files via api
    @PostMapping
    @HasRole({RoleEnum.PUPIL, RoleEnum.SPECIALIST, RoleEnum.ADMIN})
    public ResponseEntity<StoredFileDTO> upload(
            @RequestParam MultipartFile file,
            @RequestParam FileKind kind,
            @RequestAttribute("accountId") Long accountId) throws Exception {
        storedFileService.assertCanUpload(kind, accountId);
        StoredFile stored = storedFileService.store(file, kind, accountId);
        return ResponseEntity.status(HttpStatus.CREATED).body(fileUrlService.toDto(stored));

    }
    // Filtering (for ADMIN)
    @GetMapping
    @HasRole(RoleEnum.ADMIN)
    public ResponseEntity<Page<StoredFileDTO>> list(
            @RequestParam(required = false) FileKind kind,
            @RequestParam(required = false) String accountEmail,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) throws AccountNotFoundException {
        return ResponseEntity.ok(
                storedFileService.search(kind, accountEmail, from, to, pageable)
                        .map(fileUrlService::toDto));
    }
}
