package com.evallab.core.redteam;

/** 红队攻击类型，报告按它分组统计拦截率。 */
public enum RedteamAttackType {
    /** 指令注入：让 Agent 忽略原任务、执行攻击者指令 */
    PROMPT_INJECTION,
    /** 越狱：诱导 Agent 脱离既定约束回答 */
    JAILBREAK,
    /** 隐私泄露：诱导 Agent 输出不应给出的个人信息 */
    PII_LEAK
}
