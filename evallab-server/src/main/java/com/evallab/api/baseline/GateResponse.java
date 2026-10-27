package com.evallab.api.baseline;

import com.evallab.api.common.IdJson;
import com.evallab.core.baseline.GateOutcome;
import com.evallab.core.baseline.RegressionComparator;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 门禁结果，字段对齐 docs/总体方案.md 第 10 节。
 *
 * <p>不套 {@code Result}：这是给 CI 消费的机器接口，通过与否直接由 HTTP 状态码
 * （200 / 422）表达，再套一层 code 反而多一个判断维度。
 */
@Schema(description = "回归门禁结果")
public record GateResponse(
        boolean passed,
        String reason,
        @Schema(description = "按指标名索引的变化明细") Map<String, MetricChangeResponse> changes,
        @JsonSerialize(using = IdJson.class) Long taskId,
        @JsonSerialize(using = IdJson.class) Long baselineId,
        @Schema(description = "基线与当前任务来自不同 Agent，结论仅供参考")
        boolean crossAgent
) {

    @Schema(description = "单指标变化")
    public record MetricChangeResponse(
            String label,
            BigDecimal baselineValue,
            BigDecimal currentValue,
            BigDecimal delta,
            BigDecimal ratio,
            BigDecimal threshold,
            boolean regressed,
            String detail
    ) {
        static MetricChangeResponse of(RegressionComparator.MetricChange c) {
            return new MetricChangeResponse(c.label(), c.baselineValue(), c.currentValue(), c.delta(),
                    c.ratio(), c.threshold(), c.regressed(), c.detail());
        }
    }

    public static GateResponse of(GateOutcome outcome) {
        Map<String, MetricChangeResponse> changes = new LinkedHashMap<>();
        outcome.result().changes().forEach(c -> changes.put(c.metric(), MetricChangeResponse.of(c)));
        return new GateResponse(outcome.result().passed(), outcome.result().reason(), changes,
                outcome.task().taskId(), outcome.baseline().baselineId(), outcome.crossAgent());
    }
}
