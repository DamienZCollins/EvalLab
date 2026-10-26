-- 阶段 1：Trace 两层模型（trace > span），events 并入 span 的 JSON 字段

CREATE TABLE eval_trace
(
    id           BIGINT       NOT NULL COMMENT '主键，雪花 ID',
    trace_id     VARCHAR(64)  NOT NULL COMMENT 'Agent 生成的全局唯一 ID，幂等键',
    agent_name   VARCHAR(128) NOT NULL COMMENT '上报方 Agent 名称',
    status       VARCHAR(16)  NOT NULL COMMENT 'SUCCESS / FAILED / TIMEOUT',
    output       MEDIUMTEXT   NULL COMMENT 'Agent 最终输出',
    total_tokens INT          NOT NULL DEFAULT 0 COMMENT '总 token 数',
    duration_ms  BIGINT       NOT NULL DEFAULT 0 COMMENT '总耗时毫秒',
    source       VARCHAR(16)  NOT NULL COMMENT 'REPORT 上报 / EXECUTE 评测拉取',
    created_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_eval_trace_trace_id (trace_id),
    KEY idx_eval_trace_agent_created (agent_name, created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='Trace 主记录';

CREATE TABLE eval_span
(
    id             BIGINT       NOT NULL COMMENT '主键，雪花 ID',
    trace_id       VARCHAR(64)  NOT NULL COMMENT '所属 Trace',
    span_id        VARCHAR(64)  NOT NULL COMMENT 'Trace 内唯一的 Span ID',
    parent_span_id VARCHAR(64)  NULL COMMENT '父 Span ID，顶层为 NULL',
    span_type      VARCHAR(16)  NOT NULL COMMENT 'LLM_CALL / TOOL_CALL / RETRIEVAL',
    tool_name      VARCHAR(128) NULL COMMENT '工具名，非 TOOL_CALL 时为 NULL',
    input          MEDIUMTEXT   NULL,
    output         MEDIUMTEXT   NULL,
    error          MEDIUMTEXT   NULL COMMENT '错误信息，无错误为 NULL',
    tokens         INT          NOT NULL DEFAULT 0,
    duration_ms    BIGINT       NOT NULL DEFAULT 0,
    start_time     DATETIME(3)  NULL,
    end_time       DATETIME(3)  NULL,
    events         JSON         NULL COMMENT '事件日志，元素结构见 TRACE_CONTRACT.md',
    created_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_eval_span_trace_span (trace_id, span_id),
    KEY idx_eval_span_trace (trace_id),
    KEY idx_eval_span_parent (parent_span_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='Span 记录';
