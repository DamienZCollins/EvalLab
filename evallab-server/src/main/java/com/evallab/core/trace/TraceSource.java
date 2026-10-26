package com.evallab.core.trace;

/** Trace 的来源，决定它是否参与评测（见 docs/设计决策.md D1）。 */
public enum TraceSource {
    /** Agent 主动上报，仅可查看，不参与评测 */
    REPORT,
    /** 评测任务拉取 Agent 得到，参与评测 */
    EXECUTE
}
