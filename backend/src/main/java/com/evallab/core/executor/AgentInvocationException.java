package com.evallab.core.executor;

import com.evallab.core.eval.FailureCategory;

/**
 * Agent 调用失败。
 *
 * <p>分层规则见 docs/设计决策.md D1：超时与连接失败归 ENVIRONMENT，
 * 响应不合契约、重复 trace_id 等归 UNKNOWN。
 */
public class AgentInvocationException extends RuntimeException {

    private final FailureCategory category;

    public AgentInvocationException(FailureCategory category, String message) {
        super(message);
        this.category = category;
    }

    public AgentInvocationException(FailureCategory category, String message, Throwable cause) {
        super(message, cause);
        this.category = category;
    }

    public FailureCategory category() {
        return category;
    }
}
