package com.evallab.core.trace;

import com.evallab.contract.trace.SpanDto;
import com.evallab.contract.trace.SpanType;
import com.evallab.contract.trace.TraceReport;
import com.evallab.contract.trace.TraceStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TraceValidatorTest {

    @Test
    void acceptsValidNestedTrace() {
        TraceReport report = report(List.of(
                span("p", null, SpanType.LLM_CALL, null),
                span("c", "p", SpanType.TOOL_CALL, "web_search")));

        assertThat(TraceValidator.validate(report)).isEmpty();
    }

    @Test
    void rejectsMissingTraceIdAndAgentName() {
        TraceReport report = new TraceReport(" ", "", TraceStatus.SUCCESS, "o", 0, 0L, List.of());

        assertThat(TraceValidator.validate(report))
                .contains("trace_id 不能为空", "agent_name 不能为空");
    }

    @Test
    void rejectsEmptySpanList() {
        TraceReport report = report(List.of());

        assertThat(TraceValidator.validate(report)).contains("spans 不能为空");
    }

    @Test
    void rejectsOverlongIdentifiers() {
        TraceReport report = new TraceReport("t".repeat(65), "a".repeat(129),
                TraceStatus.SUCCESS, "o", 0, 0L, List.of(span("s".repeat(65), null, SpanType.LLM_CALL, null)));

        assertThat(TraceValidator.validate(report))
                .contains("trace_id 超过最大长度 64: 65",
                        "agent_name 超过最大长度 128: 129",
                        "spans[0].span_id 超过最大长度 64: 65");
    }

    @Test
    void rejectsOverlongToolName() {
        TraceReport report = report(List.of(span("a", null, SpanType.TOOL_CALL, "x".repeat(129))));

        assertThat(TraceValidator.validate(report))
                .contains("spans[0].tool_name 超过最大长度 128: 129");
    }

    @Test
    void rejectsToolCallWithoutToolName() {
        TraceReport report = report(List.of(span("a", null, SpanType.TOOL_CALL, null)));

        assertThat(TraceValidator.validate(report))
                .contains("spans[0].tool_name 在 TOOL_CALL 时不能为空");
    }

    @Test
    void rejectsDuplicateSpanId() {
        TraceReport report = report(List.of(
                span("a", null, SpanType.LLM_CALL, null),
                span("a", null, SpanType.LLM_CALL, null)));

        assertThat(TraceValidator.validate(report)).anyMatch(v -> v.contains("span_id 重复"));
    }

    @Test
    void rejectsDanglingParent() {
        TraceReport report = report(List.of(span("a", "ghost", SpanType.LLM_CALL, null)));

        assertThat(TraceValidator.validate(report))
                .contains("spans[0].parent_span_id 指向不存在的 span: ghost");
    }

    @Test
    void rejectsSelfParent() {
        TraceReport report = report(List.of(span("a", "a", SpanType.LLM_CALL, null)));

        assertThat(TraceValidator.validate(report))
                .contains("spans[0].parent_span_id 不能指向自身");
    }

    @Test
    void rejectsCycle() {
        TraceReport report = report(List.of(
                span("a", "b", SpanType.LLM_CALL, null),
                span("b", "a", SpanType.LLM_CALL, null)));

        assertThat(TraceValidator.validate(report)).contains("span 父子关系存在环");
    }

    @Test
    void rejectsEndBeforeStart() {
        SpanDto bad = new SpanDto("a", null, SpanType.LLM_CALL, null, null, null, null, 0, 0L,
                Instant.parse("2026-10-26T10:00:01Z"), Instant.parse("2026-10-26T10:00:00Z"), List.of());

        assertThat(TraceValidator.validate(report(List.of(bad))))
                .contains("spans[0].end_time 早于 start_time");
    }

    private static TraceReport report(List<SpanDto> spans) {
        return new TraceReport("t1", "agent", TraceStatus.SUCCESS, "out", 10, 100L, spans);
    }

    private static SpanDto span(String id, String parent, SpanType type, String tool) {
        Instant start = Instant.parse("2026-10-26T10:00:00Z");
        return new SpanDto(id, parent, type, tool, "in", "out", null, 1, 10L,
                start, start.plusMillis(10), List.of());
    }
}
