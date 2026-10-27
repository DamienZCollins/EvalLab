package com.evallab.core.scorer;

import com.evallab.core.testcase.TestCase;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具调用评分器：对比 expected_tools 与 Trace 中的工具名序列（见 docs/设计决策.md D3）。
 *
 * <p>评分三档：顺序完全一致 1.0；多重集合一致但顺序不同 0.8；其余按 LCS / max(长度) 比例扣分。
 */
@Component
public class ToolScorer {

    private static final BigDecimal PERFECT = new BigDecimal("1.0");
    private static final BigDecimal SAME_SET = new BigDecimal("0.8");
    private static final String SCORE_NAME = "tool_sequence";

    private final ScorerSettings settings;

    public ToolScorer(ScorerSettings settings) {
        this.settings = settings;
    }

    /**
     * @return 该用例对应的评分记录；未声明 expected_tools 时返回 null
     */
    public ScoreRecord score(TestCase testCase, AgentOutput agentOutput) {
        List<String> expected = testCase.expectedTools();
        if (expected == null || expected.isEmpty()) {
            return null;
        }
        List<String> actual = agentOutput.toolNames() == null ? List.of() : agentOutput.toolNames();

        BigDecimal score;
        String reason;
        if (expected.equals(actual)) {
            score = PERFECT;
            reason = "工具序列完全一致";
        } else if (multiset(expected).equals(multiset(actual))) {
            score = SAME_SET;
            reason = "工具集合一致但顺序不同";
        } else {
            int lcs = longestCommonSubsequence(expected, actual);
            int max = Math.max(expected.size(), actual.size());
            score = BigDecimal.valueOf(lcs)
                    .divide(BigDecimal.valueOf(max), 3, RoundingMode.HALF_UP);
            reason = "工具序列不匹配，LCS " + lcs + "，max " + max
                    + "；期望 " + expected + "，实际 " + actual;
        }
        return ScoreRecord.judging(ScorerType.TOOL_CORRECTNESS, SCORE_NAME, score,
                settings.toolCorrectnessThreshold(), reason);
    }

    private static Map<String, Integer> multiset(List<String> tools) {
        Map<String, Integer> counts = new HashMap<>();
        for (String tool : tools) {
            counts.merge(tool, 1, Integer::sum);
        }
        return counts;
    }

    private static int longestCommonSubsequence(List<String> a, List<String> b) {
        int[][] dp = new int[a.size() + 1][b.size() + 1];
        for (int i = 1; i <= a.size(); i++) {
            for (int j = 1; j <= b.size(); j++) {
                dp[i][j] = a.get(i - 1).equals(b.get(j - 1))
                        ? dp[i - 1][j - 1] + 1
                        : Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
        }
        return dp[a.size()][b.size()];
    }
}
