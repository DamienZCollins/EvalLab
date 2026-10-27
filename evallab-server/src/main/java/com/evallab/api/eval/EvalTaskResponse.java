package com.evallab.api.eval;

import com.evallab.api.common.IdJson;
import com.evallab.core.eval.EvalTaskStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "评测任务汇总")
public record EvalTaskResponse(
        @JsonSerialize(using = IdJson.class) Long taskId,
        String agentName,
        EvalTaskStatus status,
        int totalCases,
        int passedCases,
        int failedCases,
        BigDecimal passRate,
        BigDecimal avgScore,
        Long p95DurationMs,
        @Schema(description = "回归门禁结论；null 表示未做过门禁检查")
        Boolean isRegressionDetected,
        String error,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt
) {
}
