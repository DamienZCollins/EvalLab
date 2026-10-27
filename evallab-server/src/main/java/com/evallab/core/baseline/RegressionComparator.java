package com.evallab.core.baseline;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 回归对比：把当前任务指标和基线比，判定是否退化。
 *
 * <p>三项指标口径不同，统一成「变化量 + 是否越界」：
 * <ul>
 *   <li>通过率、平均分看绝对差值（当前 - 基线），阈值是允许的最大跌幅；</li>
 *   <li>P95 耗时看倍数（当前 / 基线），阈值是允许的最大涨幅。</li>
 * </ul>
 * 指标缺失（例如基线当时没有评分记录）时该项跳过，不因为缺数据判失败——
 * 否则首次引入 Judge 之类的变更会让所有门禁无条件挂掉。
 */
public final class RegressionComparator {

    private RegressionComparator() {
    }

    /** @param thresholds 为 null 时取 {@link GateThresholds#defaults()} */
    public static GateResult compare(Baseline baseline, Baseline current, GateThresholds thresholds) {
        GateThresholds t = (thresholds == null ? GateThresholds.defaults() : thresholds).withDefaults();
        List<MetricChange> changes = new ArrayList<>();

        changes.add(absolute("pass_rate", "通过率", baseline.passRate(), current.passRate(),
                t.passRateThreshold()));
        changes.add(score("avg_score", "平均分", baseline.avgScore(), current.avgScore(),
                t.avgScoreThreshold()));
        changes.add(ratio("p95_duration_ms", "P95 耗时", baseline.p95DurationMs(), current.p95DurationMs(),
                t.p95DurationThreshold()));

        List<MetricChange> regressions = changes.stream().filter(MetricChange::regressed).toList();
        boolean passed = regressions.isEmpty();
        String reason = passed ? null : regressions.stream()
                .map(MetricChange::describe)
                .reduce((a, b) -> a + "；" + b)
                .orElse(null);
        return new GateResult(passed, reason, changes);
    }

    /**
     * 绝对值指标：跌幅超过阈值（即差值 &lt; 阈值，阈值为负）判回归。
     *
     * @param threshold 允许的最大跌幅，负数
     */
    private static MetricChange absolute(String key, String label, BigDecimal base, BigDecimal current,
                                         BigDecimal threshold) {
        if (base == null || current == null) {
            return MetricChange.skipped(key, label, base, current, threshold, "指标缺失，跳过");
        }
        BigDecimal delta = current.subtract(base);
        // 阈值是负数，delta 比它还小才算跌超
        boolean regressed = delta.compareTo(threshold) < 0;
        return new MetricChange(key, label, base, current, delta, null, threshold, regressed,
                regressed ? label + "下降 " + percent(delta.negate()) + "（" + base.toPlainString()
                        + " -> " + current.toPlainString() + "），超过阈值 " + percent(threshold.negate()) : null);
    }

    /**
     * 平均分是 0-1 的量纲，阈值也是绝对差值；reason 里用绝对差表述，
     * 免得「下降 24%」被误读成相对变化（实际是跌了 0.24 分）。
     */
    private static MetricChange score(String key, String label, BigDecimal base, BigDecimal current,
                                      BigDecimal threshold) {
        MetricChange c = absolute(key, label, base, current, threshold);
        if (!c.regressed()) {
            return c;
        }
        return new MetricChange(c.metric(), c.label(), c.baselineValue(), c.currentValue(), c.delta(),
                null, c.threshold(), true,
                label + "下降 " + c.delta().negate().toPlainString() + " 分（" + base.toPlainString()
                        + " -> " + current.toPlainString() + "），超过阈值 " + threshold.negate().toPlainString());
    }

    /**
     * 倍数指标：涨幅超过阈值倍数判回归。
     *
     * @param threshold 允许的最大倍数，正数，如 1.5
     */
    private static MetricChange ratio(String key, String label, Long base, Long current, BigDecimal threshold) {
        if (base == null || current == null) {
            return MetricChange.skipped(key, label, null, null, threshold, "指标缺失，跳过");
        }
        if (base <= 0) {
            // 基线为 0 时倍数无意义（除零），无法判断退化
            return MetricChange.skipped(key, label, BigDecimal.valueOf(base), BigDecimal.valueOf(current),
                    threshold, "基线为 0，倍数无意义，跳过");
        }
        BigDecimal ratio = BigDecimal.valueOf(current)
                .divide(BigDecimal.valueOf(base), 4, RoundingMode.HALF_UP);
        boolean regressed = ratio.compareTo(threshold) > 0;
        return new MetricChange(key, label, BigDecimal.valueOf(base), BigDecimal.valueOf(current),
                null, ratio, threshold, regressed,
                regressed ? label + "增加 " + percent(ratio.subtract(BigDecimal.ONE)) + "（" + base
                        + "ms -> " + current + "ms），超过阈值 " + percent(threshold.subtract(BigDecimal.ONE))
                        : null);
    }

    private static String percent(BigDecimal value) {
        return value.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    /**
     * 单指标对比结果。
     *
     * @param delta 绝对差值（当前 - 基线），仅绝对指标有
     * @param ratio 倍数（当前 / 基线），仅耗时指标有
     */
    public record MetricChange(
            String metric,
            String label,
            BigDecimal baselineValue,
            BigDecimal currentValue,
            BigDecimal delta,
            BigDecimal ratio,
            BigDecimal threshold,
            boolean regressed,
            String detail
    ) {
        static MetricChange skipped(String key, String label, BigDecimal base, BigDecimal current,
                                    BigDecimal threshold, String reason) {
            return new MetricChange(key, label, base, current, null, null, threshold, false, reason);
        }

        String describe() {
            return detail == null ? label : detail;
        }
    }

    /**
     * @param passed     是否通过门禁
     * @param reason     未通过时的一句话归因；通过时为 null
     * @param changes    三项指标明细，含被跳过的
     */
    public record GateResult(boolean passed, String reason, List<MetricChange> changes) {
    }
}
