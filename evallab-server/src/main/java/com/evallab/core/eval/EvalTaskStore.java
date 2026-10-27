package com.evallab.core.eval;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** 评测任务与结果的持久化端口。 */
public interface EvalTaskStore {

    EvalTask createTask(String agentName, int totalCases);

    void markRunning(long taskId);

    void completeTask(long taskId, TaskSummary summary);

    void failTask(long taskId, String error);

    Optional<EvalTask> findTask(long taskId);

    /** 记录门禁结论；每次执行门禁都覆盖上次结果。 */
    void markRegression(long taskId, boolean detected);

    EvalResult saveResult(EvalResult result);

    List<EvalResult> findResults(long taskId);

    /** 按结果 ID 查询；人工抽检按结果逐条比对时使用。 */
    Optional<EvalResult> findResult(long resultId);

    /** 任务汇总指标，由执行结束时一次性写入。 */
    record TaskSummary(int passedCases, int failedCases, BigDecimal passRate,
                       BigDecimal avgScore, Long p95DurationMs) {
    }
}
