package com.evallab.api.testcase;

import com.evallab.api.common.IdJson;
import com.evallab.contract.trace.FaultInjection;
import com.evallab.core.testcase.MatchType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "用例详情")
public record TestCaseResponse(
        @JsonSerialize(using = IdJson.class) Long testcaseId,
        @JsonSerialize(using = IdJson.class) Long groupId,
        String name,
        String input,
        MatchType matchType,
        String expectedOutput,
        List<String> expectedTools,
        FaultInjection faultInjection,
        List<String> tags,
        int version,
        boolean enabled
) {
}
