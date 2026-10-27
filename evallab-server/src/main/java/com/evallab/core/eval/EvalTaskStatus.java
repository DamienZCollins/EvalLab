package com.evallab.core.eval;

/** 评测任务状态：PENDING → RUNNING → COMPLETED / FAILED。 */
public enum EvalTaskStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}
