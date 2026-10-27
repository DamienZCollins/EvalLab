package com.evallab.core.review;

import java.util.List;
import java.util.Optional;

/** 人工抽检的持久化端口。 */
public interface ManualReviewStore {

    /** 同一 result_id 重复提交时覆盖旧记录（唯一键约束保证一人一结果）。 */
    ManualReview save(ManualReviewDraft draft);

    Optional<ManualReview> findByResultId(long resultId);

    List<ManualReview> findAll();
}
