package com.evallab.core.review;

import com.evallab.core.eval.EvalResult;
import com.evallab.core.eval.EvalTaskStore;
import com.evallab.core.eval.FailureCategory;
import com.evallab.core.scorer.ScoreRecord;
import com.evallab.core.scorer.ScorerType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 人工抽检：收录人工判断，并据此计算 Judge 准确率与归因准确率。
 *
 * <p>这里只做「人对机器」的比对，不反过来修正评分——校准的产出是指标，不是自动调参。
 */
@Service
public class ManualReviewService {

    private final ManualReviewStore reviewStore;
    private final EvalTaskStore taskStore;

    public ManualReviewService(ManualReviewStore reviewStore, EvalTaskStore taskStore) {
        this.reviewStore = reviewStore;
        this.taskStore = taskStore;
    }

    public ManualReview submit(ManualReviewDraft draft) {
        if (draft.resultId() == null) {
            throw new InvalidReviewException("result_id 不能为空");
        }
        if (draft.humanScore() != null
                && (draft.humanScore().compareTo(BigDecimal.ZERO) < 0
                || draft.humanScore().compareTo(BigDecimal.ONE) > 0)) {
            throw new InvalidReviewException("human_score 必须在 0 到 1 之间");
        }
        taskStore.findResult(draft.resultId())
                .orElseThrow(() -> new InvalidReviewException("评测结果不存在: " + draft.resultId()));
        return reviewStore.save(draft);
    }

    /** @return 全部抽检记录与据此算出的准确率 */
    public AccuracyReport report() {
        List<ManualReview> reviews = reviewStore.findAll();
        int judgeSamples = 0;
        int judgeConsistent = 0;
        int attributionSamples = 0;
        int attributionConsistent = 0;
        List<ReviewComparison> comparisons = new ArrayList<>();

        for (ManualReview review : reviews) {
            EvalResult result = taskStore.findResult(review.resultId()).orElse(null);
            if (result == null) {
                // 结果被删或库被清过，跳过而不是让整个报表失败
                continue;
            }
            BigDecimal judgeScore = judgeScore(result);
            Boolean judgeAgree = null;
            if (judgeScore != null && review.humanScore() != null) {
                judgeSamples++;
                judgeAgree = judgeScore.subtract(review.humanScore()).abs()
                        .compareTo(AccuracyMetrics.JUDGE_TOLERANCE) < 0;
                if (judgeAgree) {
                    judgeConsistent++;
                }
            }
            // 每条抽检都参与归因准确率：人工没标分层即「人工认为不属于任何分层」，
            // 与系统归因为 null 同样记为一致。只统计人工标了失败的样本会漏掉误报。
            attributionSamples++;
            if (sameCategory(review.humanFailureCategory(), result.failureCategory())) {
                attributionConsistent++;
            }
            comparisons.add(new ReviewComparison(review, result.status(), result.failureReason(),
                    result.failureCategory(), judgeScore, judgeAgree));
        }

        AccuracyMetrics metrics = new AccuracyMetrics(reviews.size(), judgeSamples, judgeConsistent,
                AccuracyMetrics.rate(judgeConsistent, judgeSamples),
                attributionSamples, attributionConsistent,
                AccuracyMetrics.rate(attributionConsistent, attributionSamples));
        return new AccuracyReport(metrics, comparisons);
    }

    /** Judge 分；该结果没有 LLM_JUDGE 评分记录时为 null。 */
    static BigDecimal judgeScore(EvalResult result) {
        return result.scores().stream()
                .filter(s -> s.scorerType() == ScorerType.LLM_JUDGE)
                .map(ScoreRecord::score)
                .findFirst()
                .orElse(null);
    }

    public ManualReview find(long resultId) {
        return reviewStore.findByResultId(resultId).orElse(null);
    }

    /**
     * @param judgeAgree 人工分与 Judge 分是否一致；任一侧缺失为 null
     */
    public record ReviewComparison(ManualReview review, String systemStatus, String failureReason,
                                   FailureCategory systemCategory,
                                   BigDecimal judgeScore, Boolean judgeAgree) {
    }

    public record AccuracyReport(AccuracyMetrics metrics, List<ReviewComparison> comparisons) {
    }

    public static class InvalidReviewException extends RuntimeException {
        public InvalidReviewException(String message) {
            super(message);
        }
    }

    /** 两边都为 null（都认为不是分层失败）也算一致。 */
    static boolean sameCategory(FailureCategory a, FailureCategory b) {
        return Objects.equals(a, b);
    }
}
