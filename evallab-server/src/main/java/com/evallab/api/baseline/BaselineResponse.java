package com.evallab.api.baseline;

import com.evallab.api.common.IdJson;
import com.evallab.core.baseline.Baseline;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "基线快照")
public record BaselineResponse(
        @JsonSerialize(using = IdJson.class) Long baselineId,
        String name,
        @JsonSerialize(using = IdJson.class) Long taskId,
        String agentName,
        int totalCases,
        BigDecimal passRate,
        BigDecimal avgScore,
        Long p95DurationMs,
        Instant createdAt
) {

    public static BaselineResponse of(Baseline b) {
        return new BaselineResponse(b.baselineId(), b.name(), b.taskId(), b.agentName(), b.totalCases(),
                b.passRate(), b.avgScore(), b.p95DurationMs(), b.createdAt());
    }
}
