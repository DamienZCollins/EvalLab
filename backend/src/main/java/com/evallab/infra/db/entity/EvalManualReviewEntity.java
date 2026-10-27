package com.evallab.infra.db.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("eval_manual_review")
public class EvalManualReviewEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long reviewId;
    private Long resultId;
    private BigDecimal humanScore;
    private String humanFailureCategory;
    private String reviewer;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getReviewId() {
        return reviewId;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }

    public Long getResultId() {
        return resultId;
    }

    public void setResultId(Long resultId) {
        this.resultId = resultId;
    }

    public BigDecimal getHumanScore() {
        return humanScore;
    }

    public void setHumanScore(BigDecimal humanScore) {
        this.humanScore = humanScore;
    }

    public String getHumanFailureCategory() {
        return humanFailureCategory;
    }

    public void setHumanFailureCategory(String humanFailureCategory) {
        this.humanFailureCategory = humanFailureCategory;
    }

    public String getReviewer() {
        return reviewer;
    }

    public void setReviewer(String reviewer) {
        this.reviewer = reviewer;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
