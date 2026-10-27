package com.evallab.core.eval;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TaskMetricsTest {

    @Test
    void emptyResultsProduceZeroSummary() {
        EvalTaskStore.TaskSummary summary = TaskMetrics.summarize(List.of());

        assertThat(summary.passRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.avgScore()).isNull();
    }

    @Test
    void summaryCountsPassAndAveragesScores() {
        List<EvalResult> results = List.of(result(true, "0.8", 100), result(false, "0.4", 200));

        EvalTaskStore.TaskSummary summary = TaskMetrics.summarize(results);

        assertThat(summary.passedCases()).isEqualTo(1);
        assertThat(summary.failedCases()).isEqualTo(1);
        assertThat(summary.passRate()).isEqualByComparingTo(new BigDecimal("0.5000"));
        assertThat(summary.avgScore()).isEqualByComparingTo(new BigDecimal("0.6000"));
    }

    @Test
    void resultsWithoutScoreDoNotDiluteAverage() {
        List<EvalResult> results = List.of(result(true, "1.0", 10), result(false, null, 20));

        EvalTaskStore.TaskSummary summary = TaskMetrics.summarize(results);

        assertThat(summary.avgScore()).isEqualByComparingTo(new BigDecimal("1.0000"));
    }

    @Test
    void p95UsesNearestRank() {
        // 20 个样本，第 ceil(0.95*20)=19 个（1-based）即下标 18
        List<Long> durations = java.util.stream.LongStream.rangeClosed(1, 20).boxed().toList();

        assertThat(TaskMetrics.p95(durations)).isEqualTo(19);
        assertThat(TaskMetrics.p95(List.of(500L))).isEqualTo(500);
        assertThat(TaskMetrics.p95(List.of())).isZero();
    }

    private static EvalResult result(boolean passed, String avgScore, long durationMs) {
        return new EvalResult(1L, 1L, 1L, 1, null, passed, null, null, durationMs,
                avgScore == null ? null : new BigDecimal(avgScore),
                java.time.Instant.now(), List.of());
    }
}
