package com.evallab.infra.db;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.evallab.core.eval.FailureCategory;
import com.evallab.core.review.ManualReview;
import com.evallab.core.review.ManualReviewDraft;
import com.evallab.core.review.ManualReviewStore;
import com.evallab.infra.db.entity.EvalManualReviewEntity;
import com.evallab.infra.db.mapper.EvalManualReviewMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisManualReviewStore implements ManualReviewStore {

    private final EvalManualReviewMapper mapper;

    public MybatisManualReviewStore(EvalManualReviewMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ManualReview save(ManualReviewDraft draft) {
        EvalManualReviewEntity entity = mapper.selectOne(Wrappers.<EvalManualReviewEntity>lambdaQuery()
                .eq(EvalManualReviewEntity::getResultId, draft.resultId()));
        boolean insert = entity == null;
        if (insert) {
            entity = new EvalManualReviewEntity();
            entity.setResultId(draft.resultId());
        }
        entity.setHumanScore(draft.humanScore());
        entity.setHumanFailureCategory(draft.humanFailureCategory() == null
                ? null : draft.humanFailureCategory().name());
        entity.setReviewer(draft.reviewer());
        entity.setRemark(draft.remark());
        if (insert) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return toDomain(entity);
    }

    @Override
    public Optional<ManualReview> findByResultId(long resultId) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<EvalManualReviewEntity>lambdaQuery()
                        .eq(EvalManualReviewEntity::getResultId, resultId)))
                .map(this::toDomain);
    }

    @Override
    public List<ManualReview> findAll() {
        return mapper.selectList(Wrappers.<EvalManualReviewEntity>lambdaQuery()
                        .orderByAsc(EvalManualReviewEntity::getReviewId))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private ManualReview toDomain(EvalManualReviewEntity entity) {
        return new ManualReview(entity.getReviewId(), entity.getResultId(), entity.getHumanScore(),
                entity.getHumanFailureCategory() == null ? null
                        : FailureCategory.valueOf(entity.getHumanFailureCategory()),
                entity.getReviewer(), entity.getRemark());
    }
}
