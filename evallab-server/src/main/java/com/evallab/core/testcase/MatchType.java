package com.evallab.core.testcase;

/** 用例的期望输出匹配方式，决定规则评分器语义（见 docs/设计决策.md D3）。 */
public enum MatchType {
    /** trim 后与期望文本完全一致 */
    EXACT,
    /** Java Pattern.matches 全串匹配 */
    REGEX,
    /** 输出解析为 JSON 后按 expected_output 中的 Schema 校验 */
    JSON_SCHEMA,
    /** 不产生规则评分 */
    NONE
}
