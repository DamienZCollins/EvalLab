package com.evallab.core.baseline;

import com.evallab.core.eval.EvalTask;
import com.evallab.core.eval.EvalTaskNotFoundException;
import com.evallab.core.eval.EvalTaskStatus;
import com.evallab.core.eval.EvalTaskStore;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 基线管理与回归门禁。
 *
 * <p>基线只能从已完成任务快照，保证三项指标都已落库。
 */
@Service
public class BaselineService {

    private final BaselineStore baselineStore;
    private final EvalTaskStore taskStore;

    public BaselineService(BaselineStore baselineStore, EvalTaskStore taskStore) {
        this.baselineStore = baselineStore;
        this.taskStore = taskStore;
    }

    /**
     * @param name 为空时用「Agent 名 + 完成时间」生成
     */
    public Baseline create(long taskId, String name) {
        EvalTask task = completedTask(taskId);
        if (baselineStore.findByTask(taskId).isPresent()) {
            throw new IllegalArgumentException("该任务已有基线，请勿重复创建: " + taskId);
        }
        String resolved = name == null || name.isBlank()
                ? task.agentName() + " @ " + task.completedAt()
                : name.trim();
        return baselineStore.create(resolved, taskId, task.agentName(), task.totalCases(),
                task.passRate(), task.avgScore(), task.p95DurationMs());
    }

    public List<Baseline> list() {
        return baselineStore.findAll();
    }

    public GateOutcome gate(long taskId, long baselineId, GateThresholds thresholds) {
        EvalTask task = completedTask(taskId);
        Baseline baseline = baselineStore.find(baselineId)
                .orElseThrow(() -> new BaselineNotFoundException(baselineId));

        Baseline current = new Baseline(null, "current", taskId, task.agentName(), task.totalCases(),
                task.passRate(), task.avgScore(), task.p95DurationMs(), null);
        RegressionComparator.GateResult result = RegressionComparator.compare(baseline, current, thresholds);
        taskStore.markRegression(taskId, !result.passed());

        boolean crossAgent = !baseline.agentName().equals(task.agentName());
        return new GateOutcome(result, baseline, task, crossAgent);
    }

    private EvalTask completedTask(long taskId) {
        EvalTask task = taskStore.findTask(taskId)
                .orElseThrow(() -> new EvalTaskNotFoundException(taskId));
        if (task.status() != EvalTaskStatus.COMPLETED) {
            throw new IllegalArgumentException("任务未完成，无法用于基线或回归对比: " + task.status());
        }
        return task;
    }
}
