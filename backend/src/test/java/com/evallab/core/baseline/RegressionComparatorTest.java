package com.evallab.core.baseline;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegressionComparatorTest {

    private static final GateThresholds DEFAULTS = GateThresholds.defaults();

    @Test
    void passesWhenMetricsUnchanged() {
        RegressionComparator.GateResult result = RegressionComparator.compare(
                baseline("0.85", "0.88", 1200L), current("0.85", "0.88", 1200L), DEFAULTS);
        assertTrue(result.passed());
        assertNull(result.reason());
        assertTrue(result.changes().stream().noneMatch(RegressionComparator.MetricChange::regressed));
    }

    @Test
    void detectsPassRateDropBeyondThreshold() {
        RegressionComparator.GateResult result = RegressionComparator.compare(
                baseline("0.85", "0.88", 1200L), current("0.70", "0.88", 1200L), DEFAULTS);
        assertFalse(result.passed());
        assertTrue(result.reason().contains("通过率"), result.reason());
        RegressionComparator.MetricChange passRate = metric(result, "pass_rate");
        assertEquals(new BigDecimal("-0.15"), passRate.delta());
    }

    @Test
    void allowsPassRateDropWithinThreshold() {
        // 恰好 -0.10 不算「超过」：边界按严格小于判定
        RegressionComparator.GateResult result = RegressionComparator.compare(
                baseline("0.85", "0.88", 1200L), current("0.75", "0.88", 1200L), DEFAULTS);
        assertTrue(result.passed(), result.reason());
    }

    @Test
    void detectsAvgScoreDrop() {
        RegressionComparator.GateResult result = RegressionComparator.compare(
                baseline("0.85", "0.88", 1200L), current("0.85", "0.68", 1200L), DEFAULTS);
        assertFalse(result.passed());
        assertTrue(result.reason().contains("平均分"), result.reason());
        // 平均分阈值是绝对差值，reason 也按绝对分表述
        assertTrue(result.reason().contains("0.20 分"), result.reason());
    }

    @Test
    void detectsP95RatioIncrease() {
        RegressionComparator.GateResult result = RegressionComparator.compare(
                baseline("0.85", "0.88", 1200L), current("0.85", "0.88", 1900L), DEFAULTS);
        assertFalse(result.passed());
        assertTrue(result.reason().contains("P95"), result.reason());
    }

    @Test
    void allowsP95IncreaseUpToRatio() {
        // 恰好 1.5 倍不算「超过」：边界按严格大于判定
        RegressionComparator.GateResult result = RegressionComparator.compare(
                baseline("0.85", "0.88", 1200L), current("0.85", "0.88", 1800L), DEFAULTS);
        assertTrue(result.passed(), result.reason());
        assertEquals(0, metric(result, "p95_duration_ms").ratio().compareTo(new BigDecimal("1.5000")));
    }

    @Test
    void skipsMissingMetricsInsteadOfFailing() {
        RegressionComparator.GateResult result = RegressionComparator.compare(
                baseline("0.85", null, 1200L), current("0.85", null, 1200L), DEFAULTS);
        assertTrue(result.passed());
        assertNull(metric(result, "avg_score").delta());
    }

    @Test
    void skipsRatioWhenBaselineP95IsZero() {
        RegressionComparator.GateResult result = RegressionComparator.compare(
                baseline("0.85", "0.88", 0L), current("0.85", "0.88", 5000L), DEFAULTS);
        assertTrue(result.passed());
        assertNull(metric(result, "p95_duration_ms").ratio());
    }

    @Test
    void partialThresholdsFallBackToDefaults() {
        GateThresholds partial = new GateThresholds(new BigDecimal("-0.50"), null, null);
        RegressionComparator.GateResult result = RegressionComparator.compare(
                baseline("0.85", "0.88", 1200L), current("0.60", "0.70", 1900L),
                partial.withDefaults());
        // 通过率跌 25%，阈值放宽到 50% 不拦；平均分与 P95 用默认值，仍拦
        assertTrue(metric(result, "pass_rate").regressed() == false);
        assertTrue(metric(result, "avg_score").regressed());
        assertTrue(metric(result, "p95_duration_ms").regressed());
    }

    @Test
    void nullThresholdsFallBackToDefaults() {
        // 请求体省略 thresholds 时 GateRequest 传 null，不能 NPE
        RegressionComparator.GateResult result = RegressionComparator.compare(
                baseline("0.85", "0.88", 1200L), current("0.70", "0.88", 1200L), null);
        assertFalse(result.passed());
    }

    @Test
    void rejectsNonPositiveP95Threshold() {
        try {
            new GateThresholds(null, null, BigDecimal.ZERO);
            throw new AssertionError("应拒绝非正数倍率");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("p95_duration_threshold"));
        }
    }

    private static RegressionComparator.MetricChange metric(RegressionComparator.GateResult result, String key) {
        return result.changes().stream().filter(c -> c.metric().equals(key)).findFirst().orElseThrow();
    }

    private static Baseline baseline(String passRate, String avgScore, long p95) {
        return new Baseline(1L, "base", 10L, "agent", 10,
                passRate == null ? null : new BigDecimal(passRate),
                avgScore == null ? null : new BigDecimal(avgScore), p95, null);
    }

    private static Baseline current(String passRate, String avgScore, long p95) {
        return new Baseline(2L, "current", 20L, "agent", 10,
                passRate == null ? null : new BigDecimal(passRate),
                avgScore == null ? null : new BigDecimal(avgScore), p95, null);
    }
}
