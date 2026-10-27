package com.evallab.core.baseline;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 基线：某一任务完成时的指标快照。
 *
 * @param taskId  快照来源任务；同一任务只能有一条基线
 */
public record Baseline(
        Long baselineId,
        String name,
        Long taskId,
        String agentName,
        int totalCases,
        BigDecimal passRate,
        BigDecimal avgScore,
        Long p95DurationMs,
        Instant createdAt
) {
}
