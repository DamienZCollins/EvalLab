package com.evallab.core.scorer;

import com.evallab.core.testcase.MatchType;
import com.evallab.core.testcase.TestCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JudgeScorerTest {

    @Test
    void judgeRecordIsReferenceOnlyAndNeverJudging() {
        JudgeScorer scorer = new JudgeScorer(beanFactory(model(new BigDecimal("0.42"), "一般")).getBeanProvider(JudgeModel.class));

        ScoreRecord record = scorer.score(testCase("expected"), new AgentOutput("actual", List.of()));

        assertThat(record.scorerType()).isEqualTo(ScorerType.LLM_JUDGE);
        assertThat(record.score()).isEqualByComparingTo("0.42");
        assertThat(record.reason()).isEqualTo("一般");
        assertThat(record.passed()).isNull();
        assertThat(record.threshold()).isNull();
        assertThat(record.scorerType().judging()).isFalse();
    }

    @Test
    void scoreOutsideRangeIsClamped() {
        JudgeScorer over = new JudgeScorer(beanFactory(model(new BigDecimal("1.7"), "高"))
                .getBeanProvider(JudgeModel.class));
        JudgeScorer under = new JudgeScorer(beanFactory(model(new BigDecimal("-0.3"), "低"))
                .getBeanProvider(JudgeModel.class));

        assertThat(over.score(testCase("expected"), new AgentOutput("actual", List.of())).score())
                .isEqualByComparingTo(BigDecimal.ONE);
        assertThat(under.score(testCase("expected"), new AgentOutput("actual", List.of())).score())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void skipsWhenTestcaseHasNoExpectedOutput() {
        JudgeScorer scorer = new JudgeScorer(beanFactory(model(BigDecimal.ONE, "ok"))
                .getBeanProvider(JudgeModel.class));

        assertThat(scorer.score(testCase(null), new AgentOutput("actual", List.of()))).isNull();
        assertThat(scorer.score(testCase("  "), new AgentOutput("actual", List.of()))).isNull();
    }

    @Test
    void failedJudgeCallYieldsNullInsteadOfBreakingTheRun() {
        JudgeModel broken = (input, expected, actual) -> {
            throw new JudgeModel.JudgeUnavailableException("upstream 503", null);
        };
        JudgeScorer scorer = new JudgeScorer(beanFactory(broken).getBeanProvider(JudgeModel.class));

        assertThat(scorer.score(testCase("expected"), new AgentOutput("actual", List.of()))).isNull();
    }

    @Test
    void noModelConfiguredMeansNoRecord() {
        JudgeScorer scorer = new JudgeScorer(new DefaultListableBeanFactory().getBeanProvider(JudgeModel.class));

        assertThat(scorer.score(testCase("expected"), new AgentOutput("actual", List.of()))).isNull();
    }

    @Test
    void judgeScoreDoesNotAffectPassFail() {
        RuleScorer ruleScorer = new RuleScorer(new com.fasterxml.jackson.databind.ObjectMapper());
        ToolScorer toolScorer = new ToolScorer(new ScorerSettings(null));
        JudgeScorer judgeScorer = new JudgeScorer(beanFactory(model(BigDecimal.ZERO, "很差"))
                .getBeanProvider(JudgeModel.class));
        ScoringPipeline pipeline = new ScoringPipeline(ruleScorer, toolScorer, judgeScorer);

        // 规则评分通过、Judge 给出 0 分：按 D2 仍应 PASS
        ScoringPipeline.Verdict verdict = pipeline.score(testCase("actual"),
                new AgentOutput("actual", List.of()));

        assertThat(verdict.passed()).isTrue();
        assertThat(verdict.records()).hasSize(2);
        assertThat(verdict.records()).anyMatch(r -> r.scorerType() == ScorerType.LLM_JUDGE);
    }

    private static DefaultListableBeanFactory beanFactory(JudgeModel model) {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        factory.registerSingleton("judgeModel", model);
        return factory;
    }

    private static JudgeModel model(BigDecimal score, String reason) {
        return (input, expected, actual) -> new JudgeModel.JudgeVerdict(score, reason);
    }

    private static TestCase testCase(String expectedOutput) {
        return new TestCase(1L, null, "case", "input", MatchType.EXACT, expectedOutput,
                null, null, List.of(), 1, true);
    }
}
