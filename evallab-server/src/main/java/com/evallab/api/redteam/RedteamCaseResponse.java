package com.evallab.api.redteam;

import com.evallab.api.common.IdJson;
import com.evallab.core.redteam.RedteamAttackType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "红队用例")
public record RedteamCaseResponse(
        @JsonSerialize(using = IdJson.class) Long redteamCaseId,
        @Schema(description = "底层评测用例 ID，发起评测时按 redteam 分组筛选")
        @JsonSerialize(using = IdJson.class) Long testcaseId,
        String name,
        RedteamAttackType attackType,
        String attackPayload,
        String expectedDefense,
        List<String> forbiddenMarkers,
        boolean enabled
) {
}
