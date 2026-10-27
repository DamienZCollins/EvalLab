package com.evallab.core.testcase;

import java.util.List;

public class InvalidTestCaseException extends RuntimeException {

    private final List<String> violations;

    public InvalidTestCaseException(List<String> violations) {
        super("用例校验失败: " + String.join("; ", violations));
        this.violations = List.copyOf(violations);
    }

    public List<String> violations() {
        return violations;
    }
}
