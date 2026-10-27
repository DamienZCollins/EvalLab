package com.evallab.core.eval;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 任务级指标计算：通过率、平均分、P95 耗时（见 docs/总体方案.md 第 10 节）。 */
public final class TaskMetrics {

    private TaskMetrics() {
    }

    public static EvalTaskStore.TaskSummary summarize(List<EvalResult> results) {
        if (results.isEmpty()) {
            return new EvalTaskStore.TaskSummary(0, 0, BigDecimal.ZERO, null, null);
        }
        int passed = (int) results.stream().filter(EvalResult::passed).count();
        int failed = results.size() - passed;
        BigDecimal passRate = BigDecimal.valueOf(passed)
                .divide(BigDecimal.valueOf(results.size()), 4, RoundingMode.HALF_UP);

        List<EvalResult> withScore = results.stream().filter(r -> r.avgScore() != null).toList();
        BigDecimal avgScore = withScore.isEmpty() ? null
                : withScore.stream().map(EvalResult::avgScore).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(withScore.size()), 4, RoundingMode.HALF_UP);

        return new EvalTaskStore.TaskSummary(passed, failed, passRate, avgScore,
                p95(results.stream().map(EvalResult::durationMs).toList()));
    }

    /**
     * 最近秩法（nearest-rank）P95：排序后取第 ceil(0.95 * n) 个。
     *
     * <p>小样本下比线性插值更稳定，且取值必然来自真实观测，便于和人工核对。
     */
    public static long p95(List<Long> durations) {
        if (durations == null || durations.isEmpty()) {
            return 0;
        }
        List<Long> sorted = new ArrayList<>(durations);
        Collections.sort(sorted);
        int rank = (int) Math.ceil(0.95 * sorted.size());
        return sorted.get(Math.max(0, Math.min(sorted.size() - 1, rank - 1)));
    }
}
