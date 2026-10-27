package com.evallab.core.baseline;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 门禁阈值，语义见 docs/总体方案.md 第 10 节。
 *
 * <p>三个指标都是「相对基线的变化量」，正负号含义一致：负数代表变差。
 *
 * @param passRateThreshold     通过率允许的最大跌幅，如 -0.10 表示跌超 10 个百分点即拦截
 * @param avgScoreThreshold     平均分允许的最大跌幅，如 -0.15
 * @param p95DurationThreshold  P95 耗时允许的最大倍数，如 1.5 表示涨超 50% 即拦截
 */
public record GateThresholds(
        BigDecimal passRateThreshold,
        BigDecimal avgScoreThreshold,
        BigDecimal p95DurationThreshold
) {

    /** 默认值取自总体方案：通过率 -10%、平均分 -0.15、P95 +50%。 */
    public static GateThresholds defaults() {
        return new GateThresholds(new BigDecimal("-0.10"), new BigDecimal("-0.15"), new BigDecimal("1.5"));
    }

    /** 未提供的字段用默认值补齐，避免调用方必须写全三项。 */
    public GateThresholds withDefaults() {
        GateThresholds d = defaults();
        return new GateThresholds(
                Objects.requireNonNullElse(passRateThreshold, d.passRateThreshold()),
                Objects.requireNonNullElse(avgScoreThreshold, d.avgScoreThreshold()),
                Objects.requireNonNullElse(p95DurationThreshold, d.p95DurationThreshold()));
    }

    public GateThresholds {
        if (p95DurationThreshold != null && p95DurationThreshold.signum() <= 0) {
            throw new IllegalArgumentException("p95_duration_threshold 必须是正数（倍率），收到 " + p95DurationThreshold);
        }
    }
}
