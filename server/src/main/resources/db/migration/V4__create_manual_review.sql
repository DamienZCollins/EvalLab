-- 人工抽检：每条记录对照一个评测结果，记录人工判断，用于计算 Judge 与归因准确率
CREATE TABLE eval_manual_review
(
    review_id              BIGINT        NOT NULL COMMENT '主键，雪花 ID',
    result_id              BIGINT        NOT NULL COMMENT '被抽检的评测结果',
    human_score            DECIMAL(4, 3) NULL COMMENT '人工给出的 0-1 分；未标注为 NULL',
    human_failure_category VARCHAR(32)   NULL COMMENT '人工判定的失败分层；通过或未标注为 NULL',
    reviewer               VARCHAR(64)   NULL COMMENT '抽检人',
    remark                 TEXT          NULL COMMENT '备注',
    created_at             DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at             DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (review_id),
    UNIQUE KEY uk_eval_manual_review_result (result_id),
    KEY idx_eval_manual_review_category (human_failure_category)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='人工抽检';
