package com.evallab.core.redteam;

import java.util.List;

/** 创建或更新红队用例的入参；底层 eval_testcase 的判定字段由服务层按禁用标记生成。 */
public record RedteamCaseDraft(
        String name,
        RedteamAttackType attackType,
        String attackPayload,
        String expectedDefense,
        List<String> forbiddenMarkers,
        Boolean enabled
) {
}
