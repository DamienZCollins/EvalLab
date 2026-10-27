package com.evallab.cli;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GateReportTest {

    private static final CliArgs ARGS = new CliArgs(1L, 2L, null, null, null, "http://localhost:8080");

    @Test
    void printsFailureWithRegressionMarkers() {
        List<GateResult.Metric> metrics = List.of(
                new GateResult.Metric("pass_rate", "通过率", new BigDecimal("0.85"), new BigDecimal("0.70"),
                        new BigDecimal("-0.15"), null, new BigDecimal("-0.10"), true, null),
                new GateResult.Metric("p95_duration_ms", "P95 耗时", new BigDecimal("1200"),
                        new BigDecimal("1350"), null, new BigDecimal("1.1250"), new BigDecimal("1.5"),
                        false, null));
        String out = render(new GateResult(false, "通过率下降 15.0%", metrics, 422));
        assertTrue(out.contains("❌ 回归检测失败"));
        assertTrue(out.contains("0.8500 -> 0.7000 (-15%)"), out);
        assertTrue(out.contains("[阈值: -10%]"), out);
        assertTrue(out.contains("1200ms -> 1350ms (+12.5%)"), out);
        assertTrue(out.contains("[阈值: +50%]"), out);
        assertTrue(out.contains("原因：通过率下降 15.0%"));
    }

    @Test
    void printsPassWithoutReason() {
        List<GateResult.Metric> metrics = List.of(
                new GateResult.Metric("pass_rate", "通过率", new BigDecimal("0.85"), new BigDecimal("0.87"),
                        new BigDecimal("0.02"), null, new BigDecimal("-0.10"), false, null));
        String out = render(new GateResult(true, null, metrics, 200));
        assertTrue(out.contains("✅ 回归检测通过"));
        assertFalse(out.contains("原因："));
        assertTrue(out.contains("(+2%)"), out);
    }

    @Test
    void printsPlaceholderForSkippedMetric() {
        List<GateResult.Metric> metrics = List.of(
                new GateResult.Metric("avg_score", "平均分", null, null, null, null,
                        new BigDecimal("-0.15"), false, "指标缺失，跳过"));
        String out = render(new GateResult(true, null, metrics, 200));
        assertTrue(out.contains("指标缺失，跳过"));
    }

    private static String render(GateResult result) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        GateReport.print(result, ARGS, new PrintStream(buffer, true, StandardCharsets.UTF_8));
        return buffer.toString(StandardCharsets.UTF_8);
    }
}
