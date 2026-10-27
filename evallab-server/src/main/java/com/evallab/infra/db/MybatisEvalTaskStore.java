package com.evallab.infra.db;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.evallab.core.eval.EvalResult;
import com.evallab.core.eval.EvalTask;
import com.evallab.core.eval.EvalTaskStatus;
import com.evallab.core.eval.EvalTaskStore;
import com.evallab.core.eval.FailureCategory;
import com.evallab.core.scorer.ScoreRecord;
import com.evallab.core.scorer.ScorerType;
import com.evallab.infra.db.entity.EvalResultEntity;
import com.evallab.infra.db.entity.EvalScoreEntity;
import com.evallab.infra.db.entity.EvalTaskEntity;
import com.evallab.infra.db.mapper.EvalResultMapper;
import com.evallab.infra.db.mapper.EvalScoreMapper;
import com.evallab.infra.db.mapper.EvalTaskMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Repository
public class MybatisEvalTaskStore implements EvalTaskStore {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final EvalTaskMapper taskMapper;
    private final EvalResultMapper resultMapper;
    private final EvalScoreMapper scoreMapper;

    public MybatisEvalTaskStore(EvalTaskMapper taskMapper, EvalResultMapper resultMapper,
                                EvalScoreMapper scoreMapper) {
        this.taskMapper = taskMapper;
        this.resultMapper = resultMapper;
        this.scoreMapper = scoreMapper;
    }

    @Override
    public EvalTask createTask(String agentName, int totalCases) {
        EvalTaskEntity entity = new EvalTaskEntity();
        entity.setAgentName(agentName);
        entity.setStatus(EvalTaskStatus.PENDING.name());
        entity.setTotalCases(totalCases);
        entity.setPassedCases(0);
        entity.setFailedCases(0);
        taskMapper.insert(entity);
        return toDomain(entity);
    }

    @Override
    public void markRunning(long taskId) {
        EvalTaskEntity entity = taskMapper.selectById(taskId);
        entity.setStatus(EvalTaskStatus.RUNNING.name());
        entity.setStartedAt(LocalDateTime.now(ZONE));
        taskMapper.updateById(entity);
    }

    @Override
    public void completeTask(long taskId, TaskSummary summary) {
        EvalTaskEntity entity = taskMapper.selectById(taskId);
        entity.setStatus(EvalTaskStatus.COMPLETED.name());
        entity.setPassedCases(summary.passedCases());
        entity.setFailedCases(summary.failedCases());
        entity.setPassRate(summary.passRate());
        entity.setAvgScore(summary.avgScore());
        entity.setP95DurationMs(summary.p95DurationMs());
        entity.setCompletedAt(LocalDateTime.now(ZONE));
        taskMapper.updateById(entity);
    }

    @Override
    public void failTask(long taskId, String error) {
        EvalTaskEntity entity = taskMapper.selectById(taskId);
        entity.setStatus(EvalTaskStatus.FAILED.name());
        entity.setError(error);
        entity.setCompletedAt(LocalDateTime.now(ZONE));
        taskMapper.updateById(entity);
    }

    @Override
    public Optional<EvalTask> findTask(long taskId) {
        return Optional.ofNullable(taskMapper.selectById(taskId)).map(this::toDomain);
    }

    @Override
    @Transactional
    public EvalResult saveResult(EvalResult result) {
        EvalResultEntity entity = new EvalResultEntity();
        entity.setTaskId(result.taskId());
        entity.setTestcaseId(result.testcaseId());
        entity.setTestcaseVersion(result.testcaseVersion());
        entity.setTraceId(result.traceId());
        entity.setStatus(result.status());
        entity.setFailureReason(result.failureReason());
        entity.setFailureCategory(result.failureCategory() == null ? null : result.failureCategory().name());
        entity.setDurationMs(result.durationMs());
        entity.setAvgScore(result.avgScore());
        resultMapper.insert(entity);

        for (ScoreRecord record : result.scores()) {
            EvalScoreEntity score = new EvalScoreEntity();
            score.setResultId(entity.getResultId());
            score.setScorerType(record.scorerType().name());
            score.setScoreName(record.scoreName());
            score.setScore(record.score());
            score.setIsPassed(record.passed());
            score.setThreshold(record.threshold());
            score.setReason(record.reason());
            scoreMapper.insert(score);
        }
        return findResult(entity.getResultId());
    }

    @Override
    public List<EvalResult> findResults(long taskId) {
        return resultMapper.selectList(Wrappers.<EvalResultEntity>lambdaQuery()
                        .eq(EvalResultEntity::getTaskId, taskId)
                        .orderByAsc(EvalResultEntity::getCreatedAt))
                .stream()
                .map(e -> findResult(e.getResultId()))
                .toList();
    }

    private EvalResult findResult(long resultId) {
        EvalResultEntity entity = resultMapper.selectById(resultId);
        List<ScoreRecord> scores = scoreMapper.selectList(Wrappers.<EvalScoreEntity>lambdaQuery()
                        .eq(EvalScoreEntity::getResultId, resultId)
                        .orderByAsc(EvalScoreEntity::getScoreId))
                .stream()
                .map(s -> new ScoreRecord(ScorerType.valueOf(s.getScorerType()), s.getScoreName(),
                        s.getScore(), s.getThreshold(), s.getIsPassed(), s.getReason()))
                .toList();
        return new EvalResult(entity.getResultId(), entity.getTaskId(), entity.getTestcaseId(),
                entity.getTestcaseVersion(), entity.getTraceId(),
                "PASS".equals(entity.getStatus()), entity.getFailureReason(),
                entity.getFailureCategory() == null ? null : FailureCategory.valueOf(entity.getFailureCategory()),
                entity.getDurationMs() == null ? 0 : entity.getDurationMs(), entity.getAvgScore(),
                toInstant(entity.getCreatedAt()), scores);
    }

    private EvalTask toDomain(EvalTaskEntity entity) {
        return new EvalTask(entity.getTaskId(), entity.getAgentName(),
                EvalTaskStatus.valueOf(entity.getStatus()),
                entity.getTotalCases() == null ? 0 : entity.getTotalCases(),
                entity.getPassedCases() == null ? 0 : entity.getPassedCases(),
                entity.getFailedCases() == null ? 0 : entity.getFailedCases(),
                entity.getPassRate(), entity.getAvgScore(), entity.getP95DurationMs(),
                entity.getError(), toInstant(entity.getStartedAt()), toInstant(entity.getCompletedAt()),
                toInstant(entity.getCreatedAt()));
    }

    private static Instant toInstant(LocalDateTime time) {
        return time == null ? null : time.atZone(ZONE).toInstant();
    }
}
