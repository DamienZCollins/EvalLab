# EvalLab

独立的 Agent 离线评测平台：从 Trace 采集到回归门禁的完整闭环。

> 当前进度：阶段 2（用例管理 + 规则/工具评分 + 异步评测引擎 + Web 界面 + Docker Compose 一键启动）。

## 核心能力（规划）

1. **完整 Trace 采集**：两层模型（trace > span），支持嵌套与工具调用链
2. **多维度自动评分**：规则评分 + 工具调用评分 + LLM-as-Judge
3. **失败归因分层**：5 层自动归因（输入理解 / 规划 / 工具调用 / 输出生成 / 数据环境）
4. **Judge 校准体系**：人工抽检对照组，计算 Judge 准确率与归因准确率
5. **回归门禁**：基线对比 + 阈值判断，指标退化时以退出码 1 拦截 CI

详见 [总体方案](docs/总体方案.md) 与 [开发路径](docs/开发路径.md)，已定案的设计决策见 [设计决策](docs/设计决策.md)。

## 技术栈

| 类别 | 选型 |
|------|------|
| 语言 | Java 21 |
| 后端 | Spring Boot 3.5.1 + MyBatis-Plus 3.5.9 + Flyway |
| 数据库 | MySQL 8.0.40 |
| LLM | Spring AI 1.1.0（OpenAI 兼容接口） |
| 前端 | Vue 3.5 + Element Plus（ECharts 于阶段 3 接入） |
| 本地环境 | Docker Compose |

不使用 Redis / Elasticsearch / Sa-Token（单用户、初期数据量小，见总体方案）。

## 目录结构

```text
EvalLab/
├── evallab-contract/        # 平台与 Agent 共用契约：Trace DTO、/execute 请求体，无框架依赖
├── evallab-server/          # 后端服务
│   └── src/main/
│       ├── java/com/evallab/
│       │   ├── api/         # REST 接口层：common / trace / testcase / eval
│       │   ├── core/        # 核心领域：trace / testcase / scorer / eval / executor
│       │   └── infra/       # 基础设施：db（MyBatis-Plus）/ agent（HTTP 调用）/ seed
│       └── resources/       # 配置与 db/migration 迁移脚本
├── evallab-agent-demo/      # 故障注入 Agent，模拟完整 Trace 并支持 5 类故障
├── evallab-web/             # 前端：用例管理页 + 评测任务页
├── docker/                  # docker-compose.yml（mysql / server / agent / web）+ 初始化脚本
├── docs/                    # 方案、开发路径、设计决策、Trace 契约、编码约定
└── .env.example             # 服务端环境变量模板
```

## 快速开始

### 方式一：Docker Compose 一键启动（推荐）

```bash
cp docker/.env.example docker/.env
```

编辑 `docker/.env` 设置 `MYSQL_ROOT_PASSWORD`，然后：

```bash
docker compose -f docker/docker-compose.yml --env-file docker/.env up -d --build
```

首次构建需拉取镜像和 Maven/npm 依赖，约几分钟；之后启动约 30 秒内四个服务就绪。

| 服务 | 地址 |
|------|------|
| Web 界面 | http://localhost:5173 |
| 后端接口 | http://localhost:8080 |
| 接口文档 | http://localhost:8080/swagger-ui.html |
| 故障注入 Agent | http://localhost:8081 |

首次启动时 Flyway 自动建表，并按需写入 10 个演示用例（5 正常 + 5 故障）。

停止：

```bash
docker compose -f docker/docker-compose.yml --env-file docker/.env down
```

数据卷 `evallab_mysql-data` 保留；需彻底清库时追加 `-v`。

### 方式二：本地开发（分进程调试）

1. **启动 MySQL**：`docker compose -f docker/docker-compose.yml --env-file docker/.env up -d mysql`
2. **配置并导出服务端环境变量**：

   ```bash
   cp .env.example .env
   ```

   编辑 `.env`，密码与 `docker/.env` 保持一致：

   ```bash
   set -a && source .env && set +a
   ```

3. **构建并启动后端与 Agent**：

   ```bash
   mvn -DskipTests package
   ```

   ```bash
   java -jar evallab-server/target/evallab-server-0.1.0-SNAPSHOT.jar
   ```

   ```bash
   java -jar evallab-agent-demo/target/fault-injection-agent.jar
   ```

4. **启动前端**：

   ```bash
   cd evallab-web && npm install && npm run dev
   ```

   Vite 开发服务器监听 `5173`，把 `/api` 代理到 `EVALLAB_SERVER_URL`（默认 `http://localhost:8080`）。

## 评测流程

1. **用例**：`input` + 判定依据。判定依据为 `expected_output`（EXACT / REGEX / JSON_SCHEMA）与 `expected_tools`（期望工具序列）的任意组合，至少一项，见 [D3](docs/设计决策.md)。
2. **创建任务**：`POST /api/eval/task`，指定 Agent 与用例筛选条件，异步执行。
3. **逐例执行**：平台主动调 Agent 的 `POST /execute`（拉取，见 [D1](docs/设计决策.md)），拿到 Trace 后评分。
4. **判定**：所有启用的判定型 scorer 均通过才算 PASS，见 [D2](docs/设计决策.md)。
5. **统计**：通过率、平均分、P95 耗时；单例结果保留 `testcase_version` 以复现。

规则评分器与工具评分器的口径：工具序列完全一致得 1.0，集合相同仅顺序不同得 0.8，其余按 LCS 长度比；阈值 `evallab.eval.tool-correctness-threshold` 默认 0.8。

## 接口一览

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/trace` | Trace 上报（补充路径，不参与评测） |
| GET | `/api/trace/{traceId}` | Trace 详情 |
| GET/POST | `/api/testcase` | 用例列表 / 创建 |
| GET/PUT/DELETE | `/api/testcase/{id}` | 用例详情 / 更新（version +1）/ 删除 |
| GET/POST | `/api/testcase/group` | 分组列表 / 创建 |
| POST | `/api/eval/task` | 创建评测任务 |
| GET | `/api/eval/task/{id}` | 任务状态与统计 |
| GET | `/api/eval/task/{id}/result` | 单例结果与评分明细 |

约定：响应统一 `{code, message, data}`，字段 snake_case，时间为 ISO 8601，雪花 ID 以字符串返回（超出 JS 安全整数范围）。

## 常见问题

- **Docker Desktop 启动报 `initializing Secrets Engine ... engine.sock ... The file cannot be accessed by the system`**
  `%LOCALAPPDATA%\docker-secrets-engine\engine.sock` 是残留的悬空符号链接，普通 `rm` / `Remove-Item` 无法删除。
  退出 Docker Desktop 后把整个 `docker-secrets-engine` 目录重命名，再启动即可（Docker 会重建）。
- **容器 healthy 但宿主机连不上 3306**
  WSL 端口转发未建立。执行 `wsl --shutdown`，重启 Docker Desktop 后重新 `up -d`。
- **`EVALLAB_DB_PASSWORD` 未设置导致启动失败**
  环境变量必须显式导出（`application.yml` 中无默认值，避免把密码写进仓库）。

## 配置约定

- 连接信息一律走环境变量，代码与配置文件中不出现明文密码
- `application.yml` 为公共配置，`application-{profile}.yml` 为环境覆盖（`dev` 本地、`docker` 容器）
- 所有密钥类文件（`.env`、`docker/.env`、`application-local.yml`）已在 `.gitignore` 中排除

## 开发约定

见 [docs/编码约定.md](docs/编码约定.md)，提交信息格式见 [AGENTS.md](AGENTS.md)。
