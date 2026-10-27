package com.evallab.core.executor;

import com.evallab.core.eval.EvalTask;
import com.evallab.core.eval.EvalTaskStore;
import com.evallab.core.testcase.TestCase;
import com.evallab.core.testcase.TestCaseQuery;
import com.evallab.core.testcase.TestCaseService;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 创建并异步执行评测任务。
 *
 * <p>第一版用线程池，任务状态存数据库（不使用 Redis）。
 */
@Service
public class EvalService {

    private final TestCaseService testCaseService;
    private final EvalTaskStore taskStore;
    private final TaskRunner taskRunner;
    private final TaskExecutor evalTaskExecutor;

    public EvalService(TestCaseService testCaseService, EvalTaskStore taskStore,
                       TaskRunner taskRunner, TaskExecutor evalTaskExecutor) {
        this.testCaseService = testCaseService;
        this.taskStore = taskStore;
        this.taskRunner = taskRunner;
        this.evalTaskExecutor = evalTaskExecutor;
    }

    /**
     * @param query 用例筛选条件，null 表示全部启用用例
     */
    public EvalTask submit(String agentName, TestCaseQuery query) {
        List<TestCase> testCases = testCaseService.query(query == null ? TestCaseQuery.all() : query);
        if (testCases.isEmpty()) {
            throw new IllegalArgumentException("没有匹配的启用用例，无法创建评测任务");
        }
        EvalTask task = taskStore.createTask(agentName, testCases.size());
        evalTaskExecutor.execute(() -> taskRunner.run(task.taskId(), agentName, testCases));
        return task;
    }

    public EvalTask getTask(long taskId) {
        return taskStore.findTask(taskId)
                .orElseThrow(() -> new com.evallab.core.eval.EvalTaskNotFoundException(taskId));
    }
}
