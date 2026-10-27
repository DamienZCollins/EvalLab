package com.evallab.core.review;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 抽检准确率。
 *
 * <p>Judge 一致的口径：|Judge 分 - 人工分| &lt; 0.1（见开发路径阶段 3）。
 * 归因一致的口径：系统归因与人工归因完全相同；两边都为 null（判定通过）也算一致。
 *
 * @param judgeSamples      有 Judge 分与人工分的样本数
 * @param judgeConsistent   其中一致的样本数
 * @param judgeAccuracy     一致率；样本为 0 时为 null
 * @param attributionSamples    同时有系统归因与人工归因的样本数
 * @param attributionConsistent 其中一致的样本数
 * @param attributionAccuracy   一致率；样本为 0 时为 null
 */
public record AccuracyMetrics(
        int totalReviews,
        int judgeSamples,
        int judgeConsistent,
        BigDecimal judgeAccuracy,
        int attributionSamples,
        int attributionConsistent,
        BigDecimal attributionAccuracy
) {

    /** Judge 分差小于该值视为一致。 */
    public static final BigDecimal JUDGE_TOLERANCE = new BigDecimal("0.1");

    public static BigDecimal rate(int consistent, int samples) {
        if (samples <= 0) {
            return null;
        }
        return BigDecimal.valueOf(consistent)
                .divide(BigDecimal.valueOf(samples), 4, RoundingMode.HALF_UP);
    }
}
