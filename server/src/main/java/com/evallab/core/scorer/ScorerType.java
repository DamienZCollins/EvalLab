package com.evallab.core.scorer;

/**
 * 评分器类型。
 *
 * <p>判定型（RULE / TOOL_CORRECTNESS）参与 PASS/FAIL；参考型（LLM_JUDGE）不参与，
 * 见 docs/设计决策.md D2。
 */
public enum ScorerType {
    RULE(true),
    TOOL_CORRECTNESS(true),
    LLM_JUDGE(false);

    private final boolean judging;

    ScorerType(boolean judging) {
        this.judging = judging;
    }

    public boolean judging() {
        return judging;
    }
}
