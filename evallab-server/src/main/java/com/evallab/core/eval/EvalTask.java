package com.evallab.core.eval;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 评测任务汇总。
 *
 * @param avgScore 各结果平均分的均值（等权对待用例）
 */
public record EvalTask(
        Long taskId,
        String agentName,
        EvalTaskStatus status,
        int totalCases,
        int passedCases,
        int failedCases,
        BigDecimal passRate,
        BigDecimal avgScore,
        Long p95DurationMs,
        String error,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt
) {
}
