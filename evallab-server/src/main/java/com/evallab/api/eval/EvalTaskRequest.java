package com.evallab.api.eval;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** 创建评测任务的请求体。 */
@Schema(description = "评测任务请求体")
public record EvalTaskRequest(
        @Schema(description = "被测 Agent 名称，需在 evallab.agent.registry 中配置") @NotBlank String agentName,
        @Schema(description = "只评测该分组的用例，可为空") Long groupId,
        @Schema(description = "只评测带该标签的用例，可为空") String tag
) {
}
