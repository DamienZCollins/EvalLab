package com.evallab.contract.trace;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Span 内的一条事件记录。
 *
 * @param eventType 事件级别
 * @param message   事件内容
 * @param timestamp 发生时间
 */
public record SpanEvent(
        @JsonProperty("event_type") EventType eventType,
        @JsonProperty("message") String message,
        @JsonProperty("timestamp") Instant timestamp
) {
}
