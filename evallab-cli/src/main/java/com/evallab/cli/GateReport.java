package com.evallab.cli;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 控制台输出，格式见 docs/总体方案.md 第 10 节。
 *
 * <p>三项指标逐行给出「基线 -> 当前（变化）[阈值]」，CI 日志里能一眼看出哪项越界。
 */
final class GateReport {

    private GateReport() {
    }

    static void print(GateResult result, CliArgs args, PrintStream out) {
        out.println(result.passed() ? "✅ 回归检测通过" : "❌ 回归检测失败");
        out.println();
        for (GateResult.Metric m : result.metrics()) {
            out.println(line(m));
        }
        if (!result.passed() && result.reason() != null) {
            out.println();
            out.println("原因：" + result.reason());
        }
        out.println();
        out.println("任务 " + args.taskId() + "  基线 " + args.baselineId());
    }

    private static String line(GateResult.Metric m) {
        String threshold = threshold(m);
        if (m.baselineValue() == null || m.currentValue() == null) {
            return pad(m.label()) + "—  " + (m.detail() == null ? "无数据" : m.detail()) + threshold;
        }
        return pad(m.label()) + display(m) + " -> " + displayValue(m, m.currentValue())
                + " (" + change(m) + ")" + threshold + (m.regressed() ? "  ⚠️" : "");
    }

    private static String display(GateResult.Metric m) {
        return displayValue(m, m.baselineValue());
    }

    private static String displayValue(GateResult.Metric m, BigDecimal value) {
        return isDuration(m) ? value.stripTrailingZeros().toPlainString() + "ms"
                : value.setScale(4, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * 耗时与通过率都按百分比展示（与总体方案第 10 节示例一致），平均分报绝对差值。
     */
    private static String change(GateResult.Metric m) {
        if (isDuration(m)) {
            if (m.ratio() == null) {
                return "—";
            }
            return signed(m.ratio().subtract(BigDecimal.ONE).multiply(BigDecimal.valueOf(100)), "%");
        }
        if (m.delta() == null) {
            return "—";
        }
        return isRate(m) ? signed(m.delta().multiply(BigDecimal.valueOf(100)), "%") : signed(m.delta(), "");
    }

    private static String threshold(GateResult.Metric m) {
        if (m.threshold() == null) {
            return "";
        }
        if (isDuration(m)) {
            BigDecimal percent = m.threshold().subtract(BigDecimal.ONE).multiply(BigDecimal.valueOf(100));
            return "  [阈值: +" + trim(percent) + "%]";
        }
        if (isRate(m)) {
            return "  [阈值: " + signed(m.threshold().multiply(BigDecimal.valueOf(100)), "%") + "]";
        }
        return "  [阈值: " + signed(m.threshold(), "") + "]";
    }

    private static boolean isDuration(GateResult.Metric m) {
        return "p95_duration_ms".equals(m.metric());
    }

    private static boolean isRate(GateResult.Metric m) {
        return "pass_rate".equals(m.metric());
    }

    private static String signed(BigDecimal value, String suffix) {
        BigDecimal rounded = value.setScale(2, RoundingMode.HALF_UP);
        String sign = rounded.signum() >= 0 ? "+" : "";
        return sign + trim(rounded) + suffix;
    }

    private static String trim(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        // stripTrailingZeros 会把 0.00 变成 0E-2
        return stripped.scale() < 0 ? stripped.setScale(0).toPlainString() : stripped.toPlainString();
    }

    /** 按显示宽度对齐中文标签：全角字符占两列。 */
    private static String pad(String label) {
        int width = 0;
        for (char c : label.toCharArray()) {
            width += c > 0x7F ? 2 : 1;
        }
        return label + " ".repeat(Math.max(0, 12 - width));
    }
}
