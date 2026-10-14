package com.profession.suggest.dto.dataanalys.comparison;

import lombok.Data;

import java.util.List;

@Data
public class CreateCollectionRequest {
    private String name;
    private String description;
    private String appVersion;
    private String dataSchemaVersion;
    private List<SampleItem> samples;
    @Data
    public static class SampleItem {
        private Long fileId;
        private String sampleName;
        private String description;
    }
}
