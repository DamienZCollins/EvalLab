package com.evallab.core.trace;

import java.util.List;

/** Trace 结构不合法，携带全部校验失败原因。 */
public class InvalidTraceException extends RuntimeException {

    private final List<String> violations;

    public InvalidTraceException(List<String> violations) {
        super("Trace 校验失败: " + String.join("; ", violations));
        this.violations = List.copyOf(violations);
    }

    public List<String> violations() {
        return violations;
    }
}
