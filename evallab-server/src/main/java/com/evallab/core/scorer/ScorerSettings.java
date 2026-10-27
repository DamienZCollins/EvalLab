package com.evallab.core.scorer;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * 评分阈值，集中在 evallab.eval 下（见 docs/设计决策.md D2）。
 *
 * @param toolCorrectnessThreshold 工具调用评分的通过阈值。默认 0.8：取 0.5 时，
 *                                 3 步链缺 1 步得 0.667 仍算通过，与「缺步骤即失败」矛盾
 */
@ConfigurationProperties(prefix = "evallab.eval")
public record ScorerSettings(BigDecimal toolCorrectnessThreshold) {

    public ScorerSettings {
        if (toolCorrectnessThreshold == null) {
            toolCorrectnessThreshold = new BigDecimal("0.8");
        }
    }
}
