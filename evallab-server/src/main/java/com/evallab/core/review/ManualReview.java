package com.evallab.core.review;

import com.evallab.core.eval.FailureCategory;

import java.math.BigDecimal;

/**
 * 一条人工抽检记录。
 *
 * @param humanScore           人工评分；未标注为 null
 * @param humanFailureCategory 人工判定的失败分层；未标注或人工判定通过为 null
 */
public record ManualReview(
        Long reviewId,
        Long resultId,
        BigDecimal humanScore,
        FailureCategory humanFailureCategory,
        String reviewer,
        String remark
) {
}
