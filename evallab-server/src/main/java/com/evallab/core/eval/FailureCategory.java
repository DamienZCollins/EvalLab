package com.evallab.core.eval;

/**
 * 失败分层，对应 docs/总体方案.md 第 9 节的 5 层归因。
 *
 * <p>枚举顺序即归因优先级：越靠前越先判定，匹配第一个即停止。
 * {@link #UNKNOWN} 兜底，不计入 5 层。
 */
public enum FailureCategory {
    /** 数据/环境问题：超时、连不上、依赖不可用 */
    ENVIRONMENT,
    /** 工具调用错误：工具报错，或工具序列评分 < 0.5 */
    TOOL_CALL,
    /** 输入理解错误：Agent 曲解指令，首个工具就偏离期望 */
    INPUT_MISUNDERSTAND,
    /** 规划错误：步骤遗漏或顺序错误（工具评分 0.5 ~ 阈值之间） */
    PLANNING,
    /** 输出生成错误：内容不合格式或与期望不符 */
    OUTPUT_GENERATION,
    /** 归因器无法分类 */
    UNKNOWN
}
