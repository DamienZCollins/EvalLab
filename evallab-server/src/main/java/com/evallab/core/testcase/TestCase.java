package com.evallab.core.testcase;

import com.evallab.contract.trace.FaultInjection;

import java.util.List;

/**
 * 评测用例。
 *
 * @param faultInjection 原样透传给 Agent；null 表示不注入故障
 * @param expectedTools  期望工具序列；null 或空表示不校验
 */
public record TestCase(
        Long testcaseId,
        Long groupId,
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

    public boolean judgesRule() {
        return matchType != MatchType.NONE;
    }

    public boolean judgesTools() {
        return expectedTools != null && !expectedTools.isEmpty();
    }

    /** 按 D2：用例必须至少启用一个判定型评分器。 */
    public boolean hasJudgingScorer() {
        return judgesRule() || judgesTools();
    }
}
