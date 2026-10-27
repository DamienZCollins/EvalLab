package com.evallab.api.baseline;

import com.evallab.core.baseline.GateThresholds;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** 门禁请求；阈值三项均可省略，缺省取 {@link GateThresholds#defaults()}。 */
@Schema(description = "回归门禁请求")
public record GateRequest(
        @NotNull(message = "task_id 不能为空")
        Long taskId,

        @NotNull(message = "baseline_id 不能为空")
        Long baselineId,

        Thresholds thresholds
) {

    @Schema(description = "门禁阈值，均为相对基线的变化量")
    public record Thresholds(
            @Schema(description = "通过率允许的最大跌幅，如 -0.10")
            BigDecimal passRateThreshold,
            @Schema(description = "平均分允许的最大跌幅，如 -0.15")
            BigDecimal avgScoreThreshold,
            @Schema(description = "P95 耗时允许的最大倍数，如 1.5")
            BigDecimal p95DurationThreshold
    ) {
        public GateThresholds toDomain() {
            return new GateThresholds(passRateThreshold, avgScoreThreshold, p95DurationThreshold);
        }
    }

    public GateThresholds thresholdsOrNull() {
        return thresholds == null ? null : thresholds.toDomain();
    }
}
