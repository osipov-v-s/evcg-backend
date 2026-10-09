package com.profession.suggest.services.files;

import com.profession.suggest.database.entities.files.StoredFile;
import com.profession.suggest.dto.file.StoredFileDTO;
import org.springframework.stereotype.Service;

@Service
public class FileUrlService {
    public String url(StoredFile f) {
        if (f == null) return null;
        return f.getKind().isPublic() ? "/public/" + f.getStorageKey()
                : "/api/files/" + f.getId() + "/content";
    }
    public StoredFileDTO toDto(StoredFile f) {
        if (f == null) return null;
        return StoredFileDTO.builder()
                .id(f.getId())
                .url(url(f))
                .contentType(f.getContentType())
                .sizeBytes(f.getSizeBytes())
                .originalName(f.getOriginalName())
                .build();
    }
}
