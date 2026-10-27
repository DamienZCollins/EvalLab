package com.evallab.core.scorer;

import java.util.List;

/**
 * 评分所需的 Agent 执行产物，从 Trace 中提取，避免评分器直接依赖 Trace 存储。
 *
 * @param toolNames Trace 中全部 TOOL_CALL 的 tool_name，按 start_time 升序展平
 */
public record AgentOutput(String output, List<String> toolNames) {
}
