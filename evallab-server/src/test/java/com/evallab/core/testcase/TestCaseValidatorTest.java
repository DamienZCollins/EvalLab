package com.evallab.core.testcase;

import com.evallab.contract.trace.FaultInjection;
import com.evallab.contract.trace.FaultType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TestCaseValidatorTest {

    @Test
    void acceptsCaseWithOnlyExpectedTools() {
        assertThat(TestCaseValidator.validate(draft(MatchType.NONE, null, List.of("a", "b")))).isEmpty();
        assertThat(TestCaseValidator.validateJudgingScorer(MatchType.NONE, List.of("a", "b"))).isEmpty();
    }

    @Test
    void rejectsCaseWithoutAnyJudgingScorer() {
        assertThat(TestCaseValidator.validateJudgingScorer(MatchType.NONE, List.of()))
                .hasSize(1)
                .allMatch(v -> v.contains("至少声明一种判定依据"));
    }

    @Test
    void rejectsExactWithoutExpectedOutput() {
        assertThat(TestCaseValidator.validate(draft(MatchType.EXACT, null, null)))
                .contains("match_type=EXACT 时 expected_output 不能为空");
    }

    @Test
    void rejectsNoneWithExpectedOutput() {
        assertThat(TestCaseValidator.validate(draft(MatchType.NONE, "x", List.of("a"))))
                .contains("match_type=NONE 时 expected_output 必须为空");
    }

    @Test
    void rejectsBrokenRegex() {
        assertThat(TestCaseValidator.validate(draft(MatchType.REGEX, "[", null)))
                .anyMatch(v -> v.startsWith("expected_output 不是合法正则"));
    }

    @Test
    void rejectsNonNumericTargetSpan() {
        TestCaseDraft draft = new TestCaseDraft(null, "n", "i", MatchType.EXACT, "o", null,
                new FaultInjection(FaultType.TOOL_ERROR, "abc"), null, true);

        assertThat(TestCaseValidator.validate(draft))
                .contains("fault_injection.target_span 必须是整数序号: abc");
    }

    @Test
    void rejectsBlankToolName() {
        assertThat(TestCaseValidator.validate(draft(MatchType.NONE, null, List.of("a", " "))))
                .contains("expected_tools 不能包含空工具名");
    }

    @Test
    void rejectsBlankNameAndInput() {
        TestCaseDraft draft = new TestCaseDraft(null, " ", "", MatchType.EXACT, "o", null, null, null, true);

        assertThat(TestCaseValidator.validate(draft))
                .contains("name 不能为空", "input 不能为空");
    }

    private static TestCaseDraft draft(MatchType type, String expectedOutput, List<String> expectedTools) {
        return new TestCaseDraft(null, "case", "input", type, expectedOutput, expectedTools, null, null, true);
    }
}
