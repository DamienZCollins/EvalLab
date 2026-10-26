package com.evallab.contract.trace;

/**
 * 故障注入类型。仅故障注入 Agent 消费，普通 Agent 忽略整个 fault_injection 字段。
 *
 * <p>与归因分层的对应关系见 docs/设计决策.md 与 docs/TRACE_CONTRACT.md。
 */
public enum FaultType {
    /** 工具调用返回错误 */
    TOOL_ERROR,
    /** 跳过规划中的某个步骤 */
    STEP_MISSING,
    /** 输出不符合格式要求 */
    OUTPUT_FORMAT_ERROR,
    /** 模拟超时 */
    TIMEOUT,
    /** 曲解用户指令 */
    INPUT_MISUNDERSTAND
}
