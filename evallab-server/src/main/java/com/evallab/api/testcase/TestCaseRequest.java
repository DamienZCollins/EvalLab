package com.evallab.api.testcase;

import com.evallab.contract.trace.FaultInjection;
import com.evallab.core.testcase.MatchType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/** 创建/更新用例的请求体。字段语义见 docs/设计决策.md D3。 */
@Schema(description = "用例请求体")
public record TestCaseRequest(
        @Schema(description = "所属分组，可为空") Long groupId,
        @Schema(description = "用例名称") @NotBlank String name,
        @Schema(description = "用户指令，原样传给 Agent") @NotBlank String input,
        @Schema(description = "期望输出匹配方式") MatchType matchType,
        @Schema(description = "期望输出；语义随 match_type 变化") String expectedOutput,
        @Schema(description = "期望工具名序列；空表示不校验") List<String> expectedTools,
        @Schema(description = "故障注入参数，原样透传") FaultInjection faultInjection,
        @Schema(description = "标签") List<String> tags,
        @Schema(description = "是否启用，缺省为 true") Boolean enabled
) {
}
