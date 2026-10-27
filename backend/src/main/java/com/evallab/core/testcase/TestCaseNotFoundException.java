package com.evallab.core.testcase;

public class TestCaseNotFoundException extends RuntimeException {

    public TestCaseNotFoundException(long testcaseId) {
        super("用例不存在: " + testcaseId);
    }
}
