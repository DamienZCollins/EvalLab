package com.evallab.core.redteam;

import java.util.List;

/**
 * 红队用例。
 *
 * <p>执行与评分完全复用 {@code eval_testcase}（见设计决策 D5）：{@code testcaseId} 指向的用例
 * 以 {@link RedteamGuard} 生成的守卫正则作为 {@code expected_output}，输出命中任一禁用标记即 FAIL。
 *
 * @param forbiddenMarkers 禁用标记，Agent 输出中出现任一即视为被攻破
 */
public record RedteamCase(
        Long redteamCaseId,
        Long testcaseId,
        String name,
        RedteamAttackType attackType,
        String attackPayload,
        String expectedDefense,
        List<String> forbiddenMarkers,
        boolean enabled
) {
}
