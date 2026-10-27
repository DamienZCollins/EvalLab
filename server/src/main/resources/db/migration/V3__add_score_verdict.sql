-- 评分判定结果落库：阈值可随配置演进，历史结果的判定不应被重算
ALTER TABLE eval_score
    ADD COLUMN is_passed TINYINT(1) NULL COMMENT '是否达到阈值；参考型评分为 NULL' AFTER score,
    ADD COLUMN threshold DECIMAL(4, 3) NULL COMMENT '判定阈值；参考型评分为 NULL' AFTER is_passed;
