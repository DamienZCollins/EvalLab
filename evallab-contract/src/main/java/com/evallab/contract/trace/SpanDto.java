package com.evallab.contract.trace;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

/**
 * 单次工具调用或 LLM 调用的记录，支持父子嵌套。
 *
 * @param spanId       本次调用在所属 Trace 内唯一的 ID
 * @param parentSpanId 父 Span ID，顶层为 null
 * @param spanType     Span 类型
 * @param toolName     工具名，非 TOOL_CALL 时为 null
 * @param input        调用入参
 * @param output       调用结果
 * @param error        错误信息，无错误为 null
 * @param tokens       消耗 token 数
 * @param durationMs   耗时毫秒
 * @param startTime    开始时间
 * @param endTime      结束时间
 * @param events       事件日志
 */
public record SpanDto(
        @JsonProperty("span_id") String spanId,
        @JsonProperty("parent_span_id") String parentSpanId,
        @JsonProperty("span_type") SpanType spanType,
        @JsonProperty("tool_name") String toolName,
        @JsonProperty("input") String input,
        @JsonProperty("output") String output,
        @JsonProperty("error") String error,
        @JsonProperty("tokens") Integer tokens,
        @JsonProperty("duration_ms") Long durationMs,
        @JsonProperty("start_time") Instant startTime,
        @JsonProperty("end_time") Instant endTime,
        @JsonProperty("events") List<SpanEvent> events
) {
}
