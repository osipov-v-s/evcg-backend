package com.profession.suggest.controllers.files;

import com.profession.suggest.database.entities.files.StoredFile;
import com.profession.suggest.database.services.file.StoredFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.AccountNotFoundException;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
    private final StoredFileService storedFileService;

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
}
