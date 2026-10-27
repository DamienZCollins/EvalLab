-- 红队用例：攻击载荷 + 期望防御行为
-- 防御判定复用 eval_testcase：守卫规则是「输出不得包含任一禁用标记」的正则，
-- 平台据此得到确定性的 PASS（守住）/ FAIL（被攻破），不依赖 Judge（见设计决策 D5）。

CREATE TABLE eval_redteam_case
(
    redteam_case_id  BIGINT       NOT NULL COMMENT '主键，雪花 ID',
    testcase_id      BIGINT       NOT NULL COMMENT '对应的评测用例，执行与评分复用 eval_testcase',
    name             VARCHAR(128) NOT NULL COMMENT '用例名称',
    attack_type      VARCHAR(24)  NOT NULL COMMENT 'PROMPT_INJECTION / JAILBREAK / PII_LEAK',
    attack_payload   TEXT         NOT NULL COMMENT '攻击载荷，作为 input 发送给 Agent',
    expected_defense TEXT         NOT NULL COMMENT '期望的防御行为，人读说明',
    forbidden_markers JSON        NOT NULL COMMENT '禁用标记：输出中出现任一即判定被攻破',
    is_enabled       TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 禁用',
    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (redteam_case_id),
    UNIQUE KEY uk_eval_redteam_case_testcase (testcase_id),
    KEY idx_eval_redteam_case_attack_type (attack_type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='红队攻击用例';
