package com.profession.suggest.services.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@Service
@AllArgsConstructor
@Slf4j
public class JsonParserService {
    private final ObjectMapper objectMapper;

    /**
     * Parse MultipartFile JSON into a Map for flexible access
     */
    public Map<String, Object> parseJsonToMap(MultipartFile file) throws IOException {
        String content = new String(file.getBytes());
        JsonNode root = objectMapper.readTree(content);
        return jsonNodeToMap(root);
    }

    /**
     * Parse JSON to a specific DTO class
     */
    public <T> T parseJsonToClass(MultipartFile file, Class<T> targetClass) throws IOException {
        return objectMapper.readValue(file.getInputStream(), targetClass);
    }

    /**
     * Parse JSON string to a specific DTO class
     */
    public <T> T parseJsonToClass(String jsonContent, Class<T> targetClass) throws IOException {
        return objectMapper.readValue(jsonContent, targetClass);
    }
    public <T> T parseJsonToClass(org.springframework.core.io.Resource resource, Class<T> clazz) throws java.io.IOException{
        try (InputStream is = resource.getInputStream()) {
            return objectMapper.readValue(is, clazz);
        }
    }

    /**
     * Get string value from JsonNode with fallback
     */
    public String getString(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asText() : null;
    }

    public String getString(JsonNode node, String field, String defaultValue) {
        return node.has(field) ? node.get(field).asText() : defaultValue;
    }

    /**
     * Get Long value from JsonNode with fallback
     */
    public Long getLong(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asLong() : null;
    }

    public Long getLong(JsonNode node, String field, Long defaultValue) {
        return node.has(field) ? node.get(field).asLong() : defaultValue;
    }

    /**
     * Get Double value from JsonNode with fallback
     */
    public Double getDouble(JsonNode node, String field, Double defaultValue) {
        return node.has(field) ? node.get(field).asDouble() : defaultValue;
    }

    /**
     * Get Boolean value from JsonNode with fallback
     */
    public Boolean getBoolean(JsonNode node, String field, Boolean defaultValue) {
        return node.has(field) ? node.get(field).asBoolean() : defaultValue;
    }

    /**
     * Get Integer value from JsonNode with fallback
     */
    public Integer getInteger(JsonNode node, String field, Integer defaultValue) {
        return node.has(field) ? node.get(field).asInt() : defaultValue;
    }

    /**
     * Parse ISO date string to LocalDateTime
     */
    public LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse date: {}", dateTimeStr);
            return null;
        }
    }

    /**
     * Convert JsonNode to Map (for dynamic access)
     */
    private Map<String, Object> jsonNodeToMap(JsonNode node) {
        Map<String, Object> map = new HashMap<>();
        node.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            if (value.isTextual()) {
                map.put(key, value.asText());
            } else if (value.isNumber()) {
                map.put(key, value.asDouble());
            } else if (value.isBoolean()) {
                map.put(key, value.asBoolean());
            } else if (value.isObject()) {
                map.put(key, jsonNodeToMap(value));
            } else if (value.isArray()) {
                // Handle arrays if needed
                map.put(key, value.toString());
            }
        });
        return map;
    }
}
