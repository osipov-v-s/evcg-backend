package com.profession.suggest.database.entities.files;

public enum FileKind {
    SIMULATION,
    COMPARISON_SESSION,
    COMPARISON_SAMPLE_IMAGE,
    VR_TEST_ASSET;
    public boolean isPublic() {
        return this == COMPARISON_SAMPLE_IMAGE || this == VR_TEST_ASSET;
    }
}
