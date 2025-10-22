package com.asbitech.document_ms.infra.utils;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {
    private static final ObjectMapper mapper = new ObjectMapper();

    private JsonUtils() {
    throw new IllegalStateException("Utility class");
  }

    public static String toJson(Map<String, Object> map) {
        try { return mapper.writeValueAsString(map); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    public static Map<String, Object> toMap(String json) {
        try { return mapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<>() {}); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}