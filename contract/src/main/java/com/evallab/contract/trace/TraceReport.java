package com.evallab.contract.trace;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 一次 Agent 执行的完整记录。
 *
 * <p>同时充当两个角色：平台拉取时是 {@code POST /execute} 的响应体，补充上报时是
 * {@code POST /api/trace} 的请求体。两侧共用同一份定义，避免契约漂移。
 *
 * @param traceId     Agent 生成的全局唯一 ID，也是幂等键
 * @param agentName   上报方 Agent 名称
 * @param status      执行最终状态
 * @param output      Agent 最终输出
 * @param totalTokens 总 token 数
 * @param durationMs  总耗时毫秒
 * @param spans       全部 Span，可嵌套
 */
public record TraceReport(
        @JsonProperty("trace_id") String traceId,
        @JsonProperty("agent_name") String agentName,
        @JsonProperty("status") TraceStatus status,
        @JsonProperty("output") String output,
        @JsonProperty("total_tokens") Integer totalTokens,
        @JsonProperty("duration_ms") Long durationMs,
        @JsonProperty("spans") List<SpanDto> spans
) {
}
