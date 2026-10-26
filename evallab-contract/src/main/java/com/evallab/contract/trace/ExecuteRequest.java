package com.evallab.contract.trace;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 平台调用 Agent 的请求体。
 *
 * @param taskId         评测任务 ID，独立调用时可为 null
 * @param caseId         用例 ID，便于 Agent 侧日志与平台结果关联
 * @param input          用户指令
 * @param faultInjection 故障注入参数，普通 Agent 忽略
 */
public record ExecuteRequest(
        @JsonProperty("task_id") String taskId,
        @JsonProperty("case_id") String caseId,
        @JsonProperty("input") String input,
        @JsonProperty("fault_injection") FaultInjection faultInjection
) {
}
