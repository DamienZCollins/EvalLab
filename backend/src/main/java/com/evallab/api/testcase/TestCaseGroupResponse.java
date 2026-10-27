package com.evallab.api.testcase;

import com.evallab.api.common.IdJson;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "用例分组")
public record TestCaseGroupResponse(
        @JsonSerialize(using = IdJson.class) Long groupId,
        String groupName
) {
}
