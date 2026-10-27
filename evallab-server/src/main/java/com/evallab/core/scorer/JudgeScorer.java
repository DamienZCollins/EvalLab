package com.evallab.core.scorer;

import com.evallab.core.testcase.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * LLM-as-Judge：参考型评分器，不参与 PASS/FAIL（见 docs/设计决策.md D2）。
 *
 * <p>只有声明了 {@code expected_output} 的用例才有评分依据；未配置 LLM 或调用失败时
 * 返回 null，评测结果照常产出，只是缺少这一条参考信号。
 */
@Component
public class JudgeScorer {

    private static final Logger log = LoggerFactory.getLogger(JudgeScorer.class);
    private static final String SCORE_NAME = "llm_judge";

    private final ObjectProvider<JudgeModel> judgeModelProvider;

    public JudgeScorer(ObjectProvider<JudgeModel> judgeModelProvider) {
        this.judgeModelProvider = judgeModelProvider;
    }

    /**
     * @return 参考型评分记录；无期望输出、未配置 Judge 或调用失败时返回 null
     */
    public ScoreRecord score(TestCase testCase, AgentOutput agentOutput) {
        if (testCase.expectedOutput() == null || testCase.expectedOutput().isBlank()) {
            return null;
        }
        JudgeModel model = judgeModelProvider.getIfAvailable();
        if (model == null) {
            return null;
        }
        try {
            JudgeModel.JudgeVerdict verdict = model.judge(testCase.input(), testCase.expectedOutput(),
                    agentOutput.output() == null ? "" : agentOutput.output());
            BigDecimal score = clamp(verdict.score());
            return ScoreRecord.reference(ScorerType.LLM_JUDGE, SCORE_NAME, score, verdict.reason());
        } catch (RuntimeException e) {
            // Judge 是参考信号，不能因为它挂了就把整轮评测判失败
            log.warn("Judge 调用失败，跳过该用例的参考评分 testcase_id={}: {}",
                    testCase.testcaseId(), e.getMessage());
            return null;
        }
    }

    private static BigDecimal clamp(BigDecimal score) {
        if (score == null) {
            return BigDecimal.ZERO;
        }
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (score.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }
        return score;
    }
}
