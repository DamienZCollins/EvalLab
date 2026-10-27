-- 基线：某一评测任务完成时的指标快照，后续任务与它对比判断是否回归
CREATE TABLE eval_baseline
(
    baseline_id     BIGINT        NOT NULL COMMENT '主键，雪花 ID',
    name            VARCHAR(128)  NOT NULL COMMENT '基线名称',
    task_id         BIGINT        NOT NULL COMMENT '快照来源任务',
    agent_name      VARCHAR(128)  NOT NULL COMMENT '被测 Agent，用于提示跨 Agent 对比',
    total_cases     INT           NOT NULL COMMENT '快照时的用例总数',
    pass_rate       DECIMAL(6, 4) NULL COMMENT '通过率',
    avg_score       DECIMAL(6, 4) NULL COMMENT '平均分',
    p95_duration_ms BIGINT        NULL COMMENT 'P95 耗时',
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (baseline_id),
    UNIQUE KEY uk_eval_baseline_task (task_id),
    KEY idx_eval_baseline_agent (agent_name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='回归基线';

-- 任务上记录该次对比结论，便于列表页直接展示；NULL 表示未做过门禁检查
ALTER TABLE eval_task
    ADD COLUMN is_regression_detected TINYINT(1) NULL COMMENT '回归门禁结论；NULL 表示未检查' AFTER p95_duration_ms;
