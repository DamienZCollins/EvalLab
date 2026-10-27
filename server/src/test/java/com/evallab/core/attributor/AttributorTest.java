package com.evallab.core.attributor;

import com.evallab.contract.trace.TraceStatus;
import com.evallab.core.eval.FailureCategory;
import com.evallab.core.testcase.MatchType;
import com.evallab.core.testcase.TestCase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AttributorTest {

    private static final List<String> EXPECTED = List.of("web_search", "extract", "summarize");
    private static final double TOOL_THRESHOLD = 0.8;

    private final Attributor attributor = new Attributor();

    @Test
    void passedResultHasNoCategory() {
        assertThat(attributor.attribute(evidence(TraceStatus.SUCCESS, EXPECTED, List.of(""), null, 1.0, null),
                true)).isNull();
    }

    @Test
    void timeoutIsEnvironment() {
        assertThat(attribute(TraceStatus.TIMEOUT, EXPECTED, List.of(), null, 1.0, null))
                .isEqualTo(FailureCategory.ENVIRONMENT);
    }

    @Test
    void connectionKeywordInSpanErrorIsEnvironment() {
        assertThat(attribute(TraceStatus.SUCCESS, EXPECTED,
                List.of("web_search: Connection refused (localhost:9200)"), null, 1.0, null))
                .isEqualTo(FailureCategory.ENVIRONMENT);
    }

    @Test
    void spanErrorWithoutEnvironmentKeywordIsToolCall() {
        assertThat(attribute(TraceStatus.FAILED, EXPECTED,
                List.of("web_search: 工具调用失败: HTTP 500 Internal Server Error"), null, 1.0, null))
                .isEqualTo(FailureCategory.TOOL_CALL);
    }

    @Test
    void lowToolScoreIsToolCallError() {
        assertThat(attribute(TraceStatus.SUCCESS, List.of("unrelated"), List.of(), null, 0.2, null))
                .isEqualTo(FailureCategory.TOOL_CALL);
    }

    @Test
    void unrelatedFirstToolIsInputMisunderstand() {
        List<String> actual = List.of("weather_query", "summarize");
        assertThat(attribute(TraceStatus.SUCCESS, actual, List.of(), null, 0.667, null))
                .isEqualTo(FailureCategory.INPUT_MISUNDERSTAND);
    }

    @Test
    void missingMiddleStepIsPlanning() {
        List<String> actual = List.of("web_search", "summarize");
        // LCS 2 / max 3 = 0.667，落在 [0.5, 0.8) 区间
        assertThat(attribute(TraceStatus.SUCCESS, actual, List.of(), null, 0.667, null))
                .isEqualTo(FailureCategory.PLANNING);
    }

    @Test
    void ruleFailureWithCorrectToolsIsOutputGeneration() {
        assertThat(attribute(TraceStatus.SUCCESS, EXPECTED, List.of(), false, 1.0, null))
                .isEqualTo(FailureCategory.OUTPUT_GENERATION);
    }

    @Test
    void noEvidenceAtAllIsUnknown() {
        assertThat(attribute(TraceStatus.FAILED, EXPECTED, List.of(), null, null, null))
                .isEqualTo(FailureCategory.UNKNOWN);
    }

    @Test
    void toolScoreAtThresholdIsNotPlanning() {
        // 0.8 达标本身就意味着判定通过，不应被归因为规划错误
        assertThat(attribute(TraceStatus.SUCCESS, EXPECTED, List.of(), false, TOOL_THRESHOLD, null))
                .isEqualTo(FailureCategory.OUTPUT_GENERATION);
    }

    private FailureCategory attribute(TraceStatus status, List<String> tools, List<String> spanErrors,
                                      Boolean rulePassed, Double toolScore, Double threshold) {
        return attributor.attribute(
                evidence(status, tools, spanErrors, rulePassed, toolScore, threshold), false);
    }

    private static AttributionEvidence evidence(TraceStatus status, List<String> tools, List<String> spanErrors,
                                                Boolean rulePassed, Double toolScore, Double threshold) {
        TestCase testCase = new TestCase(1L, null, "case", "input", MatchType.EXACT, "expected",
                EXPECTED, null, List.of(), 1, true);
        return new AttributionEvidence(testCase, status, tools, spanErrors, "actual", rulePassed,
                toolScore, threshold == null ? TOOL_THRESHOLD : threshold, tools.isEmpty());
    }
}
