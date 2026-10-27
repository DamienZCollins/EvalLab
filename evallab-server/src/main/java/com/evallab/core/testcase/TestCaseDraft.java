package com.evallab.core.testcase;

import com.evallab.contract.trace.FaultInjection;

import java.util.List;

/** 创建或更新用例的入参。 */
public record TestCaseDraft(
        Long groupId,
        String name,
        String input,
        MatchType matchType,
        String expectedOutput,
        List<String> expectedTools,
        FaultInjection faultInjection,
        List<String> tags,
        Boolean enabled
) {
}
