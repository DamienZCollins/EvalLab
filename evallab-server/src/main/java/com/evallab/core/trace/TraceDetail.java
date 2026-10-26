package com.evallab.core.trace;

import com.evallab.contract.trace.SpanDto;
import com.evallab.contract.trace.TraceStatus;

import java.time.Instant;
import java.util.List;

/**
 * Trace 查询结果。
 *
 * @param spans 按开始时间升序展平的 Span 列表
 */
public record TraceDetail(
        String traceId,
        String agentName,
        TraceStatus status,
        String output,
        int totalTokens,
        long durationMs,
        TraceSource source,
        Instant createdAt,
        List<SpanDto> spans
) {
}
