package com.evallab.api.review;

import com.evallab.core.eval.FailureCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** 提交人工抽检的请求体。 */
@Schema(description = "人工抽检请求体")
public record ManualReviewRequest(
        @Schema(description = "被抽检的评测结果 ID") @NotNull Long resultId,
        @Schema(description = "人工评分 0-1；不评则留空") @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal humanScore,
        @Schema(description = "人工判定的失败分层；判定通过则留空") FailureCategory humanFailureCategory,
        @Schema(description = "抽检人") String reviewer,
        @Schema(description = "备注") String remark
) {
}
