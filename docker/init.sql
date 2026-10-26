-- EvalLab 本地开发库初始化
-- 仅负责建库与字符集；表结构由各阶段迁移脚本管理（阶段 1 起）

CREATE DATABASE IF NOT EXISTS `evallab`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;
