package com.profession.suggest.configuration.properties;

import com.profession.suggest.database.entities.dataanalys.prediction.PredictionTypeEnum;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "prediction.service")
public class PredictionProperties {

    /**
     * Map of prediction type -> full URL.
     * Bound from prediction.service.urls.<TYPE> in application.yml
     */
    private Map<PredictionTypeEnum, String> urls = new EnumMap<>(PredictionTypeEnum.class);

    public Map<PredictionTypeEnum, String> getUrls() {
        return urls;
    }

    public void setUrls(Map<PredictionTypeEnum, String> urls) {
        this.urls = urls;
    }
}
