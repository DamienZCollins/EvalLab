# EvalLab

独立的 Agent 离线评测平台：从 Trace 采集到回归门禁的完整闭环。

> 当前进度：阶段 0（仓库与环境）。业务代码从阶段 1 开始。

## 核心能力（规划）

1. **完整 Trace 采集**：两层模型（trace > span），支持嵌套与工具调用链
2. **多维度自动评分**：规则评分 + 工具调用评分 + LLM-as-Judge
3. **失败归因分层**：5 层自动归因（输入理解 / 规划 / 工具调用 / 输出生成 / 数据环境）
4. **Judge 校准体系**：人工抽检对照组，计算 Judge 准确率与归因准确率
5. **回归门禁**：基线对比 + 阈值判断，指标退化时以退出码 1 拦截 CI

详见 [总体方案](docs/总体方案.md) 与 [开发路径](docs/开发路径.md)。

## 技术栈

| 类别 | 选型 |
|------|------|
| 语言 | Java 21 |
| 后端 | Spring Boot 3.5.1 + MyBatis-Plus 3.5.9 |
| 数据库 | MySQL 8.0.40 |
| LLM | Spring AI 1.1.0（OpenAI 兼容接口） |
| 前端 | Vue 3.5 + Element Plus + ECharts |
| 本地环境 | Docker Compose |

不使用 Redis / Elasticsearch / Sa-Token（单用户、初期数据量小，见总体方案）。

## 目录结构

```text
EvalLab/
├── docker/                  # 本地中间件：docker-compose.yml + MySQL 初始化
├── docs/                    # 方案、开发路径、编码约定
├── evallab-server/          # 后端服务（阶段 1 起）
│   └── src/main/resources/  # 统一配置：application.yml / -dev / -example
├── evallab-web/             # 前端（阶段 2 起）
├── evallab-agent-demo/      # 示例 Agent：故障注入 / Web 搜索（阶段 1、5）
├── .env.example             # 服务端环境变量模板
└── README.md
```

## 快速开始

### 1. 启动本地 MySQL

```bash
cp docker/.env.example docker/.env
```

编辑 `docker/.env` 设置 `MYSQL_ROOT_PASSWORD`，然后：

```bash
docker compose -f docker/docker-compose.yml --env-file docker/.env up -d
```

等待健康检查通过：

```bash
docker compose -f docker/docker-compose.yml --env-file docker/.env ps
```

### 2. 配置服务端环境变量

```bash
cp .env.example .env
```

编辑 `.env`，数据库密码与 `docker/.env` 保持一致。启动服务前导出（阶段 1 起可运行）：

```bash
set -a && source .env && set +a
```

### 3. 启动服务（阶段 1 起）

```bash
cd evallab-server && mvn spring-boot:run
```

- 健康检查：`http://localhost:8080/actuator/health`
- 接口文档：`http://localhost:8080/swagger-ui.html`

### 停止 / 清理

```bash
docker compose -f docker/docker-compose.yml --env-file docker/.env down
```

数据卷 `evallab_mysql-data` 保留；需彻底清库时追加 `-v`。

## 常见问题

- **Docker Desktop 启动报 `initializing Secrets Engine ... engine.sock ... The file cannot be accessed by the system`**
  `%LOCALAPPDATA%\docker-secrets-engine\engine.sock` 是残留的悬空符号链接，普通 `rm` / `Remove-Item` 无法删除。
  退出 Docker Desktop 后把整个 `docker-secrets-engine` 目录重命名，再启动即可（Docker 会重建）。
- **容器 healthy 但宿主机连不上 3306**
  WSL 端口转发未建立。执行 `wsl --shutdown`，重启 Docker Desktop 后重新 `up -d`。

## 配置约定

- 连接信息一律走环境变量，代码与配置文件中不出现明文密码
- `application.yml` 为公共配置，`application-{profile}.yml` 为环境覆盖
- `application-example.yml` 是模板，本地复制为 `application-local.yml` 使用（不提交）
- 所有密钥类文件（`.env`、`docker/.env`、`application-local.yml`）已在 `.gitignore` 中排除

## 开发约定

见 [docs/编码约定.md](docs/编码约定.md)，提交信息格式见 [AGENTS.md](AGENTS.md)。
