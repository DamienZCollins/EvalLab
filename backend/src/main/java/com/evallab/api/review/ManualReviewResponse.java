package com.evallab.api.review;

import com.evallab.api.common.IdJson;
import com.evallab.core.eval.FailureCategory;
import com.evallab.core.review.ManualReview;
import com.evallab.core.review.ManualReviewService;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "人工抽检记录")
public record ManualReviewResponse(
        @JsonSerialize(using = IdJson.class) Long reviewId,
        @JsonSerialize(using = IdJson.class) Long resultId,
        BigDecimal humanScore,
        FailureCategory humanFailureCategory,
        String reviewer,
        String remark
) {

    public static ManualReviewResponse of(ManualReview review) {
        return new ManualReviewResponse(review.reviewId(), review.resultId(), review.humanScore(),
                review.humanFailureCategory(), review.reviewer(), review.remark());
    }

    /** 单条抽检与系统结果的对照。 */
    @Schema(description = "抽检对照")
    public record Comparison(
            ManualReviewResponse review,
            String systemStatus,
            String failureReason,
            FailureCategory systemCategory,
            BigDecimal judgeScore,
            Boolean judgeAgree
    ) {
        public static Comparison of(ManualReviewService.ReviewComparison c) {
            return new Comparison(ManualReviewResponse.of(c.review()), c.systemStatus(), c.failureReason(),
                    c.systemCategory(), c.judgeScore(), c.judgeAgree());
        }
    }

    /** 准确率报表。 */
    @Schema(description = "准确率报表")
    public record AccuracyReport(
            com.evallab.core.review.AccuracyMetrics metrics,
            List<Comparison> comparisons
    ) {
    }
}
