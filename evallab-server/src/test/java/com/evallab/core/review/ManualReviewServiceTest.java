package com.evallab.core.review;

import com.evallab.core.eval.EvalResult;
import com.evallab.core.eval.EvalTask;
import com.evallab.core.eval.EvalTaskStore;
import com.evallab.core.eval.FailureCategory;
import com.evallab.core.scorer.ScoreRecord;
import com.evallab.core.scorer.ScorerType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManualReviewServiceTest {

    private final InMemoryTaskStore taskStore = new InMemoryTaskStore();
    private final InMemoryReviewStore reviewStore = new InMemoryReviewStore();
    private final ManualReviewService service = new ManualReviewService(reviewStore, taskStore);

    @Test
    void judgeAgreementUsesAbsoluteToleranceOfPointOne() {
        taskStore.put(result(1L, true, null, "0.80"));
        taskStore.put(result(2L, true, null, "0.80"));
        taskStore.put(result(3L, true, null, "0.80"));

        service.submit(review(1L, "0.85", null));   // 差 0.05 → 一致
        service.submit(review(2L, "0.95", null));   // 差 0.15 → 不一致
        service.submit(review(3L, "0.70", null));   // 差 0.10 → 不一致（严格小于 0.1）

        AccuracyMetrics metrics = service.report().metrics();
        assertThat(metrics.judgeSamples()).isEqualTo(3);
        assertThat(metrics.judgeConsistent()).isEqualTo(1);
        assertThat(metrics.judgeAccuracy()).isEqualByComparingTo("0.3333");
    }

    @Test
    void attributionComparesNullAsAgreement() {
        taskStore.put(result(1L, true, null, "1.0"));
        taskStore.put(result(2L, false, FailureCategory.PLANNING, "0.667"));
        taskStore.put(result(3L, false, FailureCategory.PLANNING, "0.667"));

        service.submit(review(1L, "1.0", null));                       // 双方都无分层 → 一致
        service.submit(review(2L, "0.7", FailureCategory.PLANNING));   // 一致
        service.submit(review(3L, "0.7", FailureCategory.TOOL_CALL));  // 不一致

        AccuracyMetrics metrics = service.report().metrics();
        assertThat(metrics.attributionSamples()).isEqualTo(3);
        assertThat(metrics.attributionConsistent()).isEqualTo(2);
        assertThat(metrics.attributionAccuracy()).isEqualByComparingTo("0.6667");
    }

    @Test
    void missingJudgeScoreIsExcludedFromJudgeSamples() {
        taskStore.put(result(1L, true, null, null));

        service.submit(review(1L, "0.9", null));

        AccuracyMetrics metrics = service.report().metrics();
        assertThat(metrics.judgeSamples()).isZero();
        assertThat(metrics.judgeAccuracy()).isNull();
    }

    @Test
    void resubmittingSameResultOverwrites() {
        taskStore.put(result(1L, true, null, "0.5"));

        service.submit(review(1L, "0.4", null));
        service.submit(review(1L, "0.9", null));

        assertThat(reviewStore.findAll()).hasSize(1);
        assertThat(service.report().comparisons().get(0).review().humanScore())
                .isEqualByComparingTo("0.9");
    }

    @Test
    void rejectsUnknownResultAndOutOfRangeScore() {
        assertThatThrownBy(() -> service.submit(review(99L, "0.5", null)))
                .isInstanceOf(ManualReviewService.InvalidReviewException.class)
                .hasMessageContaining("评测结果不存在");

        taskStore.put(result(1L, true, null, "0.5"));
        assertThatThrownBy(() -> service.submit(review(1L, "1.4", null)))
                .isInstanceOf(ManualReviewService.InvalidReviewException.class)
                .hasMessageContaining("human_score");
    }

    private static ManualReviewDraft review(Long resultId, String score, FailureCategory category) {
        return new ManualReviewDraft(resultId, new BigDecimal(score), category, "tester", null);
    }

    private static EvalResult result(Long id, boolean passed, FailureCategory category, String judgeScore) {
        List<ScoreRecord> scores = judgeScore == null ? List.of()
                : List.of(ScoreRecord.reference(ScorerType.LLM_JUDGE, "llm_judge",
                new BigDecimal(judgeScore), "reason"));
        return new EvalResult(id, 1L, 1L, 1, "trace", passed, passed ? null : "reason", category,
                10, new BigDecimal("0.8"), Instant.now(), scores);
    }

    private static final class InMemoryTaskStore implements EvalTaskStore {
        private final Map<Long, EvalResult> results = new LinkedHashMap<>();

        void put(EvalResult result) {
            results.put(result.resultId(), result);
        }

        @Override
        public EvalTask createTask(String agentName, int totalCases) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void markRunning(long taskId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void completeTask(long taskId, TaskSummary summary) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void failTask(long taskId, String error) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<EvalTask> findTask(long taskId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public EvalResult saveResult(EvalResult result) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<EvalResult> findResults(long taskId) {
            return new ArrayList<>(results.values());
        }

        @Override
        public Optional<EvalResult> findResult(long resultId) {
            return Optional.ofNullable(results.get(resultId));
        }
    }

    private static final class InMemoryReviewStore implements ManualReviewStore {
        private final Map<Long, ManualReview> byResult = new LinkedHashMap<>();

        @Override
        public ManualReview save(ManualReviewDraft draft) {
            ManualReview review = new ManualReview((long) (byResult.size() + 1), draft.resultId(),
                    draft.humanScore(), draft.humanFailureCategory(), draft.reviewer(), draft.remark());
            byResult.put(draft.resultId(), review);
            return review;
        }

        @Override
        public Optional<ManualReview> findByResultId(long resultId) {
            return Optional.ofNullable(byResult.get(resultId));
        }

        @Override
        public List<ManualReview> findAll() {
            return byResult.values().stream()
                    .sorted(Comparator.comparing(ManualReview::reviewId))
                    .toList();
        }
    }
}
