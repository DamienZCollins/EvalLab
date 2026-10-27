package com.evallab.core.executor;

import com.evallab.contract.trace.ExecuteRequest;
import com.evallab.contract.trace.TraceReport;

/** 调用被测 Agent 的端口，实现在 infra/agent。 */
public interface AgentInvoker {

    /**
     * 按 D1 拉取一次 Agent 执行结果。
     *
     * @throws AgentInvocationException 超时、连接失败、响应不合契约
     */
    TraceReport invoke(String agentName, ExecuteRequest request);
}
