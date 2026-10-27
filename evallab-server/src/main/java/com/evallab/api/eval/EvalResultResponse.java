package com.evallab.api.eval;

import com.evallab.api.common.IdJson;
import com.evallab.core.eval.FailureCategory;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "单个用例的评测结果")
public record EvalResultResponse(
        @JsonSerialize(using = IdJson.class) Long resultId,
        @JsonSerialize(using = IdJson.class) Long testcaseId,
        int testcaseVersion,
        String traceId,
        String status,
        String failureReason,
        FailureCategory failureCategory,
        long durationMs,
        BigDecimal avgScore,
        Instant createdAt,
        List<ScoreResponse> scores
) {

    @Schema(description = "单条评分")
    public record ScoreResponse(String scorerType, String scoreName, BigDecimal score,
                                BigDecimal threshold, Boolean passed, String reason) {
    }
}
