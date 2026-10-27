package com.evallab.core.eval;

public class EvalTaskNotFoundException extends RuntimeException {

    public EvalTaskNotFoundException(long taskId) {
        super("评测任务不存在: " + taskId);
    }
}
