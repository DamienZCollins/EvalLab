package com.evallab.core.eval;

/**
 * 失败分层。
 *
 * <p>阶段 2 只有拉取失败相关的两类；阶段 3 接入完整 5 层归因后扩展。
 */
public enum FailureCategory {
    /** 超时、连接失败等环境问题 */
    ENVIRONMENT,
    /** 归因器无法分类 */
    UNKNOWN
}
