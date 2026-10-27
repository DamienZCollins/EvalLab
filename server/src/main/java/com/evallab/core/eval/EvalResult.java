package com.evallab.core.eval;

import com.evallab.core.scorer.ScoreRecord;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 单个用例的评测结果。
 *
 * @param failureCategory 拉取失败时的分层；通过或由评分判定失败时为 null（阶段 3 补齐）
 */
public record EvalResult(
        Long resultId,
        Long taskId,
        Long testcaseId,
        int testcaseVersion,
        String traceId,
        boolean passed,
        String failureReason,
        FailureCategory failureCategory,
        long durationMs,
        BigDecimal avgScore,
        Instant createdAt,
        List<ScoreRecord> scores
) {
    public String status() {
        return passed ? "PASS" : "FAIL";
    }
}
