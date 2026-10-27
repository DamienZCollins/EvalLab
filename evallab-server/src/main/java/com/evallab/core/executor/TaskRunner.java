package com.evallab.core.executor;

import com.evallab.contract.trace.SpanDto;
import com.evallab.contract.trace.SpanType;
import com.evallab.contract.trace.TraceReport;
import com.evallab.contract.trace.TraceStatus;
import com.evallab.core.attributor.AttributionEvidence;
import com.evallab.core.attributor.Attributor;
import com.evallab.core.eval.EvalResult;
import com.evallab.core.eval.EvalTaskStore;
import com.evallab.core.eval.FailureCategory;
import com.evallab.core.eval.TaskMetrics;
import com.evallab.core.scorer.AgentOutput;
import com.evallab.core.scorer.ScoringPipeline;
import com.evallab.core.testcase.TestCase;
import com.evallab.core.trace.TraceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 单任务执行器：逐用例拉取 Agent → 入库 Trace → 评分 → 落结果。
 *
 * <p>按 D1：拉取失败即 FAIL，不重试，失败用例也计入分母。
 */
@Component
public class TaskRunner {

    private static final Logger log = LoggerFactory.getLogger(TaskRunner.class);

    private final AgentInvoker agentInvoker;
    private final TraceService traceService;
    private final ScoringPipeline scoringPipeline;
    private final Attributor attributor;
    private final EvalTaskStore taskStore;

    public TaskRunner(AgentInvoker agentInvoker, TraceService traceService,
                      ScoringPipeline scoringPipeline, Attributor attributor, EvalTaskStore taskStore) {
        this.agentInvoker = agentInvoker;
        this.traceService = traceService;
        this.scoringPipeline = scoringPipeline;
        this.attributor = attributor;
        this.taskStore = taskStore;
    }

    public void run(long taskId, String agentName, List<TestCase> testCases) {
        taskStore.markRunning(taskId);
        List<EvalResult> results = new ArrayList<>();
        try {
            for (TestCase testCase : testCases) {
                results.add(runOne(taskId, agentName, testCase));
            }
            taskStore.completeTask(taskId, TaskMetrics.summarize(results));
        } catch (RuntimeException e) {
            log.error("评测任务执行失败 task_id={}", taskId, e);
            taskStore.failTask(taskId, e.getMessage());
        }
    }

    private EvalResult runOne(long taskId, String agentName, TestCase testCase) {
        Instant started = Instant.now();
        TraceReport trace;
        try {
            trace = agentInvoker.invoke(agentName,
                    new com.evallab.contract.trace.ExecuteRequest(
                            String.valueOf(taskId), String.valueOf(testCase.testcaseId()),
                            testCase.input(), testCase.faultInjection()));
        } catch (AgentInvocationException e) {
            return taskStore.saveResult(
                    failure(taskId, testCase, started, e.category(), e.getMessage()));
        }

        try {
            traceService.report(trace);
        } catch (RuntimeException e) {
            // 契约不合法或 trace_id 冲突：Agent 侧问题，归 UNKNOWN
            return taskStore.saveResult(failure(taskId, testCase, started, FailureCategory.UNKNOWN,
                    "Trace 无法入库: " + e.getMessage()));
        }

        AgentOutput agentOutput = new AgentOutput(trace.output(), toolNames(trace));
        ScoringPipeline.Verdict verdict = scoringPipeline.score(testCase, agentOutput);
        long durationMs = Duration.between(started, Instant.now()).toMillis();

        // Agent 自报 FAILED / TIMEOUT 视为明确失败，即使评分恰好达标。
        // 反向不成立：自报 SUCCESS 不足以说明通过，仍以评分器为准（见 TRACE_CONTRACT 第 5 节）。
        boolean passed = verdict.passed() && trace.status() == TraceStatus.SUCCESS;
        String failureReason = verdict.passed() && !passed
                ? "Agent 状态 " + trace.status()
                : verdict.failureReason();
        FailureCategory category = attributor.attribute(evidence(testCase, trace, agentOutput, verdict), passed);

        return taskStore.saveResult(new EvalResult(null, taskId, testCase.testcaseId(),
                testCase.version(), trace.traceId(), passed, failureReason, category,
                durationMs, verdict.avgScore(), Instant.now(), verdict.records()));
    }

    /** 归因证据只取自 Trace 与评分结果，不含 fault_injection（见 Attributor 类注释）。 */
    private static AttributionEvidence evidence(TestCase testCase, TraceReport trace,
                                               AgentOutput agentOutput, ScoringPipeline.Verdict verdict) {
        List<String> spanErrors = trace.spans() == null ? List.of() : trace.spans().stream()
                .filter(s -> s.error() != null && !s.error().isBlank())
                .map(s -> (s.toolName() == null ? s.spanType().name() : s.toolName()) + ": " + s.error())
                .toList();
        return new AttributionEvidence(testCase, trace.status(), agentOutput.toolNames(), spanErrors,
                agentOutput.output(), verdict.rulePassed(), verdict.toolScore(),
                verdict.toolThreshold(), agentOutput.toolNames().isEmpty());
    }

    /** 全部 TOOL_CALL span 按 start_time 升序展平，嵌套层级忽略（见 D3）。 */
    static List<String> toolNames(TraceReport trace) {
        if (trace.spans() == null) {
            return List.of();
        }
        return trace.spans().stream()
                .filter(s -> s.spanType() == SpanType.TOOL_CALL)
                .sorted(Comparator.comparing(SpanDto::startTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(SpanDto::toolName)
                .toList();
    }

    /**
     * 拉取阶段就失败：连不上、超时、响应不合契约，还没有 Trace 可归因，
     * 直接采用调用器给出的分层（D1）。
     */
    private EvalResult failure(long taskId, TestCase testCase, Instant started,
                               FailureCategory category, String reason) {
        return new EvalResult(null, taskId, testCase.testcaseId(), testCase.version(),
                null, false, reason, category,
                Duration.between(started, Instant.now()).toMillis(), null, Instant.now(), List.of());
    }
}
