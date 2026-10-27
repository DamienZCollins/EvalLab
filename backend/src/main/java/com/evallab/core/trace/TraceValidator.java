package com.evallab.core.trace;

import com.evallab.contract.trace.SpanDto;
import com.evallab.contract.trace.SpanType;
import com.evallab.contract.trace.TraceReport;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Trace 结构校验。只校验「存不进去 / 解释不了」的问题，不评判内容质量。
 *
 * <p>校验失败一律拒绝写入：脏数据一旦入库，后续评分与归因都要为它写兼容分支。
 */
public final class TraceValidator {

    private static final int MAX_SPANS = 1000;
    private static final int MAX_TRACE_ID = 64;
    private static final int MAX_AGENT_NAME = 128;
    private static final int MAX_SPAN_ID = 64;
    private static final int MAX_TOOL_NAME = 128;

    private TraceValidator() {
    }

    /**
     * @return 全部违规项，空列表表示通过
     */
    public static List<String> validate(TraceReport report) {
        List<String> violations = new ArrayList<>();
        if (report == null) {
            return List.of("请求体为空");
        }
        requireText(report.traceId(), "trace_id", violations);
        requireText(report.agentName(), "agent_name", violations);
        requireMaxLength(report.traceId(), "trace_id", MAX_TRACE_ID, violations);
        requireMaxLength(report.agentName(), "agent_name", MAX_AGENT_NAME, violations);
        if (report.status() == null) {
            violations.add("status 不能为空");
        }
        if (report.spans() == null) {
            violations.add("spans 不能为空");
            return violations;
        }
        if (report.spans().isEmpty()) {
            violations.add("spans 不能为空");
            return violations;
        }
        if (report.spans().size() > MAX_SPANS) {
            violations.add("spans 超过上限 " + MAX_SPANS);
        }
        validateSpans(report.spans(), violations);
        return violations;
    }

    private static void validateSpans(List<SpanDto> spans, List<String> violations) {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < spans.size(); i++) {
            SpanDto span = spans.get(i);
            String at = "spans[" + i + "]";
            if (span == null) {
                violations.add(at + " 不能为空");
                continue;
            }
            if (!requireText(span.spanId(), at + ".span_id", violations)) {
                continue;
            }
            requireMaxLength(span.spanId(), at + ".span_id", MAX_SPAN_ID, violations);
            requireMaxLength(span.parentSpanId(), at + ".parent_span_id", MAX_SPAN_ID, violations);
            requireMaxLength(span.toolName(), at + ".tool_name", MAX_TOOL_NAME, violations);
            if (!ids.add(span.spanId())) {
                violations.add(at + ".span_id 重复: " + span.spanId());
            }
            if (span.spanType() == null) {
                violations.add(at + ".span_type 不能为空");
            } else if (span.spanType() == SpanType.TOOL_CALL && !hasText(span.toolName())) {
                violations.add(at + ".tool_name 在 TOOL_CALL 时不能为空");
            }
            if (span.startTime() != null && span.endTime() != null
                    && span.endTime().isBefore(span.startTime())) {
                violations.add(at + ".end_time 早于 start_time");
            }
        }
        validateParents(spans, ids, violations);
    }

    private static void validateParents(List<SpanDto> spans, Set<String> ids, List<String> violations) {
        for (int i = 0; i < spans.size(); i++) {
            SpanDto span = spans.get(i);
            if (span == null || span.parentSpanId() == null) {
                continue;
            }
            String at = "spans[" + i + "].parent_span_id";
            if (Objects.equals(span.parentSpanId(), span.spanId())) {
                violations.add(at + " 不能指向自身");
            } else if (!ids.contains(span.parentSpanId())) {
                violations.add(at + " 指向不存在的 span: " + span.parentSpanId());
            }
        }
        if (hasCycle(spans)) {
            violations.add("span 父子关系存在环");
        }
    }

    private static boolean hasCycle(List<SpanDto> spans) {
        var parentOf = new java.util.HashMap<String, String>();
        for (SpanDto span : spans) {
            if (span != null && span.spanId() != null) {
                parentOf.put(span.spanId(), span.parentSpanId());
            }
        }
        for (String id : parentOf.keySet()) {
            Set<String> seen = new HashSet<>();
            String cursor = id;
            while (cursor != null) {
                if (!seen.add(cursor)) {
                    return true;
                }
                cursor = parentOf.get(cursor);
            }
        }
        return false;
    }

    private static boolean requireText(String value, String field, List<String> violations) {
        if (!hasText(value)) {
            violations.add(field + " 不能为空");
            return false;
        }
        return true;
    }

    private static void requireMaxLength(String value, String field, int max, List<String> violations) {
        if (value != null && value.length() > max) {
            violations.add(field + " 超过最大长度 " + max + ": " + value.length());
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
