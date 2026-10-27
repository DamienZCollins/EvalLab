package com.evallab.contract.trace;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 故障注入参数，来自用例配置。
 *
 * @param type       故障类型
 * @param targetSpan 目标 Span 序号（从 0 开始的工具调用顺序），null 表示按类型默认位置
 */
public record FaultInjection(
        @JsonProperty("type") FaultType type,
        @JsonProperty("target_span") String targetSpan
) {
}
