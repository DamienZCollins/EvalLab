-- 阶段 2：用例、分组、评测任务、结果、评分
-- 用例字段按 docs/设计决策.md D3 定义

CREATE TABLE eval_testcase_group
(
    group_id   BIGINT      NOT NULL COMMENT '主键，雪花 ID',
    group_name VARCHAR(64) NOT NULL COMMENT '分组名，全局唯一',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (group_id),
    UNIQUE KEY uk_eval_testcase_group_group_name (group_name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='用例分组（单层）';

CREATE TABLE eval_testcase
(
    testcase_id     BIGINT       NOT NULL COMMENT '主键，雪花 ID',
    group_id        BIGINT       NULL COMMENT '所属分组',
    name            VARCHAR(128) NOT NULL COMMENT '用例名称',
    input           TEXT         NOT NULL COMMENT '用户指令，原样传给 Agent',
    match_type      VARCHAR(16)  NOT NULL COMMENT 'EXACT / REGEX / JSON_SCHEMA / NONE',
    expected_output TEXT         NULL COMMENT '语义随 match_type：期望文本 / 正则 / JSON Schema 文档',
    expected_tools  JSON         NULL COMMENT '期望工具名序列；NULL 或空数组表示不校验',
    fault_injection JSON         NULL COMMENT '原样透传到 /execute；普通 Agent 忽略',
    tags            JSON         NULL COMMENT '标签数组',
    version         INT          NOT NULL DEFAULT 1 COMMENT '每次编辑 +1，评测结果快照该值',
    is_enabled      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 禁用',
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (testcase_id),
    KEY idx_eval_testcase_group (group_id),
    KEY idx_eval_testcase_version (version)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='评测用例';

CREATE TABLE eval_task
(
    task_id         BIGINT        NOT NULL COMMENT '主键，雪花 ID',
    agent_name      VARCHAR(128)  NOT NULL COMMENT '被测 Agent 名称，对应 evallab.agent.registry 的 key',
    status          VARCHAR(16)   NOT NULL COMMENT 'PENDING / RUNNING / COMPLETED / FAILED',
    total_cases     INT           NOT NULL DEFAULT 0,
    passed_cases    INT           NOT NULL DEFAULT 0,
    failed_cases    INT           NOT NULL DEFAULT 0,
    pass_rate       DECIMAL(6, 4) NULL COMMENT 'passed / total',
    avg_score       DECIMAL(6, 4) NULL COMMENT '各结果平均分的均值',
    p95_duration_ms BIGINT        NULL COMMENT 'P95 耗时',
    error           TEXT          NULL COMMENT '任务级失败原因',
    started_at      DATETIME(3)   NULL,
    completed_at    DATETIME(3)   NULL,
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (task_id),
    KEY idx_eval_task_agent_created (agent_name, created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='评测任务';

CREATE TABLE eval_result
(
    result_id        BIGINT       NOT NULL COMMENT '主键，雪花 ID',
    task_id          BIGINT       NOT NULL COMMENT '所属评测任务',
    testcase_id      BIGINT       NOT NULL COMMENT '被测用例',
    testcase_version INT          NOT NULL COMMENT '用例版本快照，保证结果可复现',
    trace_id         VARCHAR(64)  NULL COMMENT '关联 Trace；执行失败时为空',
    status           VARCHAR(8)   NOT NULL COMMENT 'PASS / FAIL',
    failure_reason   TEXT         NULL COMMENT '第一个未通过评分器的理由',
    failure_category VARCHAR(32)  NULL COMMENT 'ENVIRONMENT / UNKNOWN（阶段 3 接入完整归因）',
    duration_ms      BIGINT       NOT NULL DEFAULT 0 COMMENT '该用例墙钟耗时',
    avg_score        DECIMAL(6, 4) NULL COMMENT '该结果全部评分的均值',
    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (result_id),
    KEY idx_eval_result_task (task_id),
    KEY idx_eval_result_category (failure_category)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='评测结果';

CREATE TABLE eval_score
(
    score_id    BIGINT        NOT NULL COMMENT '主键，雪花 ID',
    result_id   BIGINT        NOT NULL COMMENT '所属评测结果',
    scorer_type VARCHAR(24)   NOT NULL COMMENT 'RULE / TOOL_CORRECTNESS / LLM_JUDGE',
    score_name  VARCHAR(64)   NOT NULL COMMENT '如 exact_match / regex / json_schema / tool_sequence',
    score       DECIMAL(4, 3) NOT NULL COMMENT '0-1 量纲',
    reason      TEXT          NULL COMMENT '评分理由',
    created_at  DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (score_id),
    KEY idx_eval_score_result (result_id),
    KEY idx_eval_score_type (scorer_type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='评分记录';
