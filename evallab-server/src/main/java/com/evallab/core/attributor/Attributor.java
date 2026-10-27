package com.evallab.core.attributor;

import com.evallab.contract.trace.TraceStatus;
import com.evallab.core.eval.FailureCategory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * 失败归因器：按 docs/总体方案.md 第 9 节的 5 层规则，自上而下匹配第一个即停止。
 *
 * <p>只使用 Trace 与评分结果作为证据，不看 {@code fault_injection}——归因要能对真实
 * Agent 生效，靠读注入参数「抄答案」的准确率没有意义。
 *
 * <p>优先级的设计理由：越底层的失败越先排除，否则会被上层症状掩盖。环境问题会让一切
 * 后续判断失真；工具报错会让输出必然出错；输入理解错误会让工具序列从第一步就偏离，
 * 必须先于「规划错误」判定，否则曲解指令会被误判成步骤遗漏。
 */
@Component
public class Attributor {

    /** 工具评分低于该值时视为「工具基本没跑对」，归工具调用错误（见 D2 三段区间）。 */
    static final double TOOL_CALL_ERROR_BELOW = 0.5;

    private static final List<String> ENVIRONMENT_KEYWORDS = List.of(
            "timeout", "timed out", "connection refused", "connection reset",
            "no route to host", "unknown host", "socket");

    /**
     * @return 失败分层；通过的结果返回 null
     */
    public FailureCategory attribute(AttributionEvidence evidence, boolean passed) {
        if (passed) {
            return null;
        }
        if (isEnvironment(evidence)) {
            return FailureCategory.ENVIRONMENT;
        }
        if (isToolCallError(evidence)) {
            return FailureCategory.TOOL_CALL;
        }
        if (isInputMisunderstand(evidence)) {
            return FailureCategory.INPUT_MISUNDERSTAND;
        }
        if (isPlanning(evidence)) {
            return FailureCategory.PLANNING;
        }
        if (Boolean.FALSE.equals(evidence.rulePassed())) {
            return FailureCategory.OUTPUT_GENERATION;
        }
        return FailureCategory.UNKNOWN;
    }

    /** 1. 数据/环境问题：Agent 自报超时，或 span 错误里出现连接/超时类关键词。 */
    private boolean isEnvironment(AttributionEvidence evidence) {
        if (evidence.traceStatus() == TraceStatus.TIMEOUT) {
            return true;
        }
        return evidence.spanErrors().stream()
                .map(e -> e.toLowerCase(Locale.ROOT))
                .anyMatch(e -> ENVIRONMENT_KEYWORDS.stream().anyMatch(e::contains));
    }

    /** 2. 工具调用错误：任一 span 报错，或工具序列评分跌到 0.5 以下。 */
    private boolean isToolCallError(AttributionEvidence evidence) {
        if (!evidence.spanErrors().isEmpty()) {
            return true;
        }
        return evidence.toolScore() != null && evidence.toolScore() < TOOL_CALL_ERROR_BELOW;
    }

    /**
     * 3. 输入理解错误：第一步就跑偏——实际执行的第一个工具既不是期望的第一个，
     * 也不在期望工具集合里。仅比对首步，因为「顺序不同」的用例本来就允许通过。
     */
    private boolean isInputMisunderstand(AttributionEvidence evidence) {
        List<String> expected = evidence.testCase().expectedTools();
        List<String> actual = evidence.toolNames();
        if (expected == null || expected.isEmpty() || actual.isEmpty()) {
            return false;
        }
        String firstExpected = expected.get(0);
        String firstActual = actual.get(0);
        return !firstActual.equals(firstExpected) && !expected.contains(firstActual);
    }

    /** 4. 规划错误：工具评分落在 [0.5, 阈值) 区间，即顺序对但步骤缺失或多了无关步骤。 */
    private boolean isPlanning(AttributionEvidence evidence) {
        if (evidence.toolScore() == null || evidence.toolThreshold() == null) {
            return false;
        }
        return evidence.toolScore() >= TOOL_CALL_ERROR_BELOW && evidence.toolScore() < evidence.toolThreshold();
    }
}
