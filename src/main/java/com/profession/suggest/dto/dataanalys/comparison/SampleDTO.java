package com.profession.suggest.dto.dataanalys.comparison;

import com.profession.suggest.dto.file.StoredFileDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SampleDTO {
    private Long id;
    private String sampleName;
    private String imagePath;
    private StoredFileDTO file;
    private String description;
    private Integer displayOrder;
}
