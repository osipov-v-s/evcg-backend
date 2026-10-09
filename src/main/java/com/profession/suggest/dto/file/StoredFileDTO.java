package com.profession.suggest.dto.file;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredFileDTO {
    private Long id;
    private String url;
    private String contentType;
    private Long sizeBytes;
    private String originalName;
}
