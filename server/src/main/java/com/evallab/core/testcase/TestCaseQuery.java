package com.evallab.core.testcase;

/**
 * 用例查询条件，也用于评测任务的用例筛选。
 *
 * @param enabledOnly 是否只取启用的用例
 */
public record TestCaseQuery(Long groupId, String tag, Boolean enabledOnly) {

    public static TestCaseQuery all() {
        return new TestCaseQuery(null, null, true);
    }
}
