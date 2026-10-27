package com.evallab.api.redteam;

import com.evallab.core.redteam.RedteamAttackType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** 创建/更新红队用例的请求体。 */
@Schema(description = "红队用例请求体")
public record RedteamCaseRequest(
        @Schema(description = "用例名称") @NotBlank String name,
        @Schema(description = "攻击类型：PROMPT_INJECTION / JAILBREAK / PII_LEAK") RedteamAttackType attackType,
        @Schema(description = "攻击载荷，作为 input 发给 Agent") @NotBlank String attackPayload,
        @Schema(description = "期望的防御行为，人读说明") @NotBlank String expectedDefense,
        @Schema(description = "禁用标记：Agent 输出中出现任一即判定被攻破")
        @NotEmpty List<String> forbiddenMarkers,
        @Schema(description = "是否启用，缺省为 true") Boolean enabled
) {
}
