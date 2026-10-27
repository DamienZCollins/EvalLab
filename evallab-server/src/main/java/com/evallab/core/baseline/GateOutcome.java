package com.evallab.core.baseline;

import com.evallab.core.eval.EvalTask;

/**
 * 门禁判定结果，连同被比较的双方一起返回，方便调用方展示「和谁比的」。
 *
 * @param crossAgent 基线与当前任务来自不同 Agent，结论需打折扣
 */
public record GateOutcome(RegressionComparator.GateResult result, Baseline baseline, EvalTask task,
                          boolean crossAgent) {
}
