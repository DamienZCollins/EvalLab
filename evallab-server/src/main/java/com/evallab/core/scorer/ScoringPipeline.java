package com.evallab.core.scorer;

import com.evallab.core.testcase.TestCase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 评分与判定编排：收集各评分器结果，按 D2 聚合出 PASS/FAIL。
 *
 * <p>PASS ⇔ 所有已启用的判定型评分器都达到阈值；Judge 当前未接入（阶段 3）。
 */
public class ScoringPipeline {

    private final RuleScorer ruleScorer;
    private final ToolScorer toolScorer;

    public ScoringPipeline(RuleScorer ruleScorer, ToolScorer toolScorer) {
        this.ruleScorer = ruleScorer;
        this.toolScorer = toolScorer;
    }

    public Verdict score(TestCase testCase, AgentOutput agentOutput) {
        List<ScoreRecord> records = new ArrayList<>();
        addIfPresent(records, ruleScorer.score(testCase, agentOutput));
        addIfPresent(records, toolScorer.score(testCase, agentOutput));
        return Verdict.of(records);
    }

    private static void addIfPresent(List<ScoreRecord> records, ScoreRecord record) {
        if (record != null) {
            records.add(record);
        }
    }

    /**
     * @param passed        判定结果
     * @param failureReason 第一个未通过评分器的理由；通过时为 null
     * @param avgScore      全部评分记录的算术均值；无评分为 null
     */
    public record Verdict(boolean passed, String failureReason, BigDecimal avgScore,
                          List<ScoreRecord> records) {

        public static Verdict of(List<ScoreRecord> records) {
            List<ScoreRecord> judging = records.stream()
                    .filter(r -> r.scorerType().judging())
                    .toList();
            ScoreRecord firstFailed = judging.stream()
                    .filter(r -> Boolean.FALSE.equals(r.passed()))
                    .findFirst()
                    .orElse(null);
            BigDecimal avg = records.isEmpty() ? null
                    : records.stream().map(ScoreRecord::score).reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(records.size()), 4, RoundingMode.HALF_UP);
            return new Verdict(firstFailed == null,
                    firstFailed == null ? null : firstFailed.reason(),
                    avg, List.copyOf(records));
        }
    }
}
