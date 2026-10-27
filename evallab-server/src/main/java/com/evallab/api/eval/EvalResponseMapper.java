package com.evallab.api.eval;

import com.evallab.core.eval.EvalResult;
import com.evallab.core.eval.EvalTask;
import org.springframework.stereotype.Component;

@Component
public class EvalResponseMapper {

    public EvalTaskResponse toResponse(EvalTask task) {
        return new EvalTaskResponse(task.taskId(), task.agentName(), task.status(),
                task.totalCases(), task.passedCases(), task.failedCases(), task.passRate(),
                task.avgScore(), task.p95DurationMs(), task.error(),
                task.startedAt(), task.completedAt(), task.createdAt());
    }

    public EvalResultResponse toResponse(EvalResult result) {
        return new EvalResultResponse(result.resultId(), result.testcaseId(), result.testcaseVersion(),
                result.traceId(), result.status(), result.failureReason(), result.failureCategory(),
                result.durationMs(), result.avgScore(), result.createdAt(),
                result.scores().stream()
                        .map(s -> new EvalResultResponse.ScoreResponse(s.scorerType().name(),
                                s.scoreName(), s.score(), s.threshold(), s.passed(), s.reason()))
                        .toList());
    }
}
