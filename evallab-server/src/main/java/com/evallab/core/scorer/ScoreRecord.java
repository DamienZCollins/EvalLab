package com.evallab.core.scorer;

import java.math.BigDecimal;

/**
 * 单条评分记录。
 *
 * @param threshold 判定阈值；参考型评分为 null
 * @param passed    是否达到阈值；参考型评分为 null
 */
public record ScoreRecord(
        ScorerType scorerType,
        String scoreName,
        BigDecimal score,
        BigDecimal threshold,
        Boolean passed,
        String reason
) {

    public static ScoreRecord judging(ScorerType type, String name, BigDecimal score,
                                      BigDecimal threshold, String reason) {
        boolean passed = score.compareTo(threshold) >= 0;
        return new ScoreRecord(type, name, score, threshold, passed, reason);
    }

    public static ScoreRecord reference(ScorerType type, String name, BigDecimal score, String reason) {
        return new ScoreRecord(type, name, score, null, null, reason);
    }
}
