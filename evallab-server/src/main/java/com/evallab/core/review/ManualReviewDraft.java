package com.evallab.core.review;

import com.evallab.core.eval.FailureCategory;

import java.math.BigDecimal;

/** 提交人工抽检的入参。 */
public record ManualReviewDraft(
        Long resultId,
        BigDecimal humanScore,
        FailureCategory humanFailureCategory,
        String reviewer,
        String remark
) {
}
