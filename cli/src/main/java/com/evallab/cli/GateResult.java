package com.evallab.cli;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 门禁响应，字段对齐 {@code api/baseline/GateResponse}。
 *
 * @param httpStatus 200 通过、422 回归；两者语义必须一致，不一致时以 HTTP 为准
 */
record GateResult(boolean passed, String reason, List<Metric> metrics, int httpStatus) {

    record Metric(String metric, String label, BigDecimal baselineValue, BigDecimal currentValue,
                  BigDecimal delta, BigDecimal ratio, BigDecimal threshold, boolean regressed, String detail) {
    }

    static GateResult from(JsonNode json, int httpStatus) {
        List<Metric> metrics = new ArrayList<>();
        JsonNode changes = json.path("changes");
        changes.fields().forEachRemaining(entry -> {
            JsonNode c = entry.getValue();
            metrics.add(new Metric(entry.getKey(), text(c, "label", entry.getKey()),
                    decimal(c, "baseline_value"), decimal(c, "current_value"),
                    decimal(c, "delta"), decimal(c, "ratio"), decimal(c, "threshold"),
                    c.path("regressed").asBoolean(false), text(c, "detail", null)));
        });
        return new GateResult(httpStatus == 200, text(json, "reason", null), metrics, httpStatus);
    }

    private static String text(JsonNode node, String field, String fallback) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? fallback : v.asText();
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.decimalValue();
    }
}
