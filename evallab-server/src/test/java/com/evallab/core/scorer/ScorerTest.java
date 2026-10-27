package com.evallab.core.scorer;

import com.evallab.core.testcase.MatchType;
import com.evallab.core.testcase.TestCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScorerTest {

    private final RuleScorer ruleScorer = new RuleScorer(new ObjectMapper());
    private final ToolScorer toolScorer = new ToolScorer(new ScorerSettings(null));

    @Test
    void exactMatchTrimsBothSides() {
        ScoreRecord score = ruleScorer.score(testCase(MatchType.EXACT, "  {\"a\":1}  ", null),
                new AgentOutput("{\"a\":1}\n", List.of()));

        assertThat(score.score()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(score.passed()).isTrue();
    }

    @Test
    void regexUsesFullMatchNotFind() {
        TestCase testCase = testCase(MatchType.REGEX, "\\d+", null);

        assertThat(ruleScorer.score(testCase, new AgentOutput("abc 123", List.of())).passed()).isFalse();
        assertThat(ruleScorer.score(testCase, new AgentOutput("123", List.of())).passed()).isTrue();
    }

    @Test
    void jsonSchemaValidatesOutput() {
        String schema = """
                {"type":"object","required":["answer","steps"],
                 "properties":{"answer":{"type":"string"},"steps":{"type":"integer"}}}
                """;
        TestCase testCase = testCase(MatchType.JSON_SCHEMA, schema, null);

        assertThat(ruleScorer.score(testCase, new AgentOutput("{\"answer\":\"x\",\"steps\":3}", List.of()))
                .passed()).isTrue();
        assertThat(ruleScorer.score(testCase, new AgentOutput("{\"answer\":", List.of()))
                .reason()).isEqualTo("输出不是合法 JSON");
        assertThat(ruleScorer.score(testCase, new AgentOutput("{\"answer\":\"x\"}", List.of()))
                .passed()).isFalse();
    }

    @Test
    void noneMatchTypeProducesNoRuleScore() {
        assertThat(ruleScorer.score(testCase(MatchType.NONE, null, List.of("a")),
                new AgentOutput("whatever", List.of("a")))).isNull();
    }

    @Test
    void toolOrderExactScoresOne() {
        ScoreRecord score = toolScorer.score(testCase(MatchType.NONE, null, List.of("a", "b")),
                new AgentOutput("o", List.of("a", "b")));

        assertThat(score.score()).isEqualByComparingTo(new BigDecimal("1.0"));
        assertThat(score.passed()).isTrue();
    }

    @Test
    void toolSameSetDifferentOrderScoresPointEight() {
        ScoreRecord score = toolScorer.score(testCase(MatchType.NONE, null, List.of("a", "b")),
                new AgentOutput("o", List.of("b", "a")));

        assertThat(score.score()).isEqualByComparingTo(new BigDecimal("0.8"));
        assertThat(score.passed()).isTrue();
    }

    @Test
    void toolMissingStepFailsAtThresholdPointEight() {
        // 期望 3 步，实际 2 步，LCS=2 → 2/3 = 0.667 < 0.8：缺步骤必须失败
        ScoreRecord twoOfThree = toolScorer.score(testCase(MatchType.NONE, null, List.of("a", "b", "c")),
                new AgentOutput("o", List.of("a", "b")));
        assertThat(twoOfThree.score()).isEqualByComparingTo(new BigDecimal("0.667"));
        assertThat(twoOfThree.passed()).isFalse();

        // 期望 4 步实际 3 步 → 0.75，仍在阈值以下
        ScoreRecord threeOfFour = toolScorer.score(testCase(MatchType.NONE, null, List.of("a", "b", "c", "d")),
                new AgentOutput("o", List.of("a", "b", "c")));
        assertThat(threeOfFour.score()).isEqualByComparingTo(new BigDecimal("0.750"));
        assertThat(threeOfFour.passed()).isFalse();

        // 期望 4 步实际 1 步 → 0.25
        ScoreRecord oneOfFour = toolScorer.score(testCase(MatchType.NONE, null, List.of("a", "b", "c", "d")),
                new AgentOutput("o", List.of("a")));
        assertThat(oneOfFour.score()).isEqualByComparingTo(new BigDecimal("0.25"));
        assertThat(oneOfFour.passed()).isFalse();
    }

    @Test
    void toolSameSetDifferentOrderStillPasses() {
        ScoreRecord score = toolScorer.score(testCase(MatchType.NONE, null, List.of("a", "b", "c")),
                new AgentOutput("o", List.of("c", "a", "b")));

        assertThat(score.score()).isEqualByComparingTo(new BigDecimal("0.8"));
        assertThat(score.passed()).isTrue();
    }

    @Test
    void toolScorerSkipsWhenExpectedEmpty() {
        assertThat(toolScorer.score(testCase(MatchType.EXACT, "o", List.of()),
                new AgentOutput("o", List.of("x")))).isNull();
    }

    @Test
    void pipelineRequiresAllJudgingScorersToPass() {
        ScoringPipeline pipeline = new ScoringPipeline(ruleScorer, toolScorer);
        TestCase testCase = testCase(MatchType.EXACT, "expected", List.of("a", "b"));

        ScoringPipeline.Verdict verdict = pipeline.score(testCase,
                new AgentOutput("expected", List.of("b", "a")));
        assertThat(verdict.passed()).isTrue();
        assertThat(verdict.records()).hasSize(2);

        ScoringPipeline.Verdict failed = pipeline.score(testCase,
                new AgentOutput("wrong", List.of("b", "a")));
        assertThat(failed.passed()).isFalse();
        assertThat(failed.failureReason()).isEqualTo("输出与期望不一致");
    }

    private static TestCase testCase(MatchType type, String expectedOutput, List<String> expectedTools) {
        return new TestCase(1L, null, "case", "input", type, expectedOutput, expectedTools,
                null, List.of(), 1, true);
    }
}
