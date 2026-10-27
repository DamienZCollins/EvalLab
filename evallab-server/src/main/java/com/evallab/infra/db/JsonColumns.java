package com.evallab.infra.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * JSON 列读写。列内容为空或损坏时返回空值而不是抛异常——历史数据的可读性优先于严格性。
 */
final class JsonColumns {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private JsonColumns() {
    }

    static String write(ObjectMapper mapper, Object value) {
        if (value == null) {
            return null;
        }
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON 列序列化失败", e);
        }
    }

    static <T> T read(ObjectMapper mapper, String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return mapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON 列反序列化失败: " + json, e);
        }
    }

    static List<String> readStringList(ObjectMapper mapper, String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return mapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON 列反序列化失败: " + json, e);
        }
    }
}
