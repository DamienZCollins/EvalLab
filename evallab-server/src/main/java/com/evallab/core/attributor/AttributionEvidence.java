package com.evallab.core.attributor;

import com.evallab.contract.trace.TraceStatus;
import com.evallab.core.testcase.TestCase;

import java.util.List;

/**
 * 归因所需的全部证据，全部来自 Trace 与评分结果——刻意不包含 {@code fault_injection}，
 * 否则归因等于直接抄答案，人工抽检算出来的准确率没有意义。
 *
 * @param traceStatus      Trace 自报状态
 * @param toolNames        实际工具序列，按 start_time 升序
 * @param spanErrors       非空 error 的 Span 描述，形如 {@code web_search: HTTP 500}
 * @param output           Agent 最终输出
 * @param rulePassed       规则评分是否通过；该用例无规则评分器时为 null
 * @param toolScore        工具评分；该用例无工具评分器时为 null
 * @param toolThreshold    工具评分阈值
 * @param emptyToolTrace   没有任何 TOOL_CALL span
 */
public record AttributionEvidence(
        TestCase testCase,
        TraceStatus traceStatus,
        List<String> toolNames,
        List<String> spanErrors,
        String output,
        Boolean rulePassed,
        Double toolScore,
        Double toolThreshold,
        boolean emptyToolTrace
) {
}
