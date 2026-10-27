# Trace 接入契约

> 版本 v1（阶段 1 锁定）。字段名一经发布不再改名，新增字段必须可选。
> 主次关系由 [设计决策 D1](设计决策.md#d1-两个接入流程的主次) 定死：**平台拉取 `/execute` 是主路径，`POST /api/trace` 上报是补充路径**。

## 1. 两种接入方式

| 方式 | 方向 | 用途 | 是否参与评测 |
|------|------|------|--------------|
| **拉取（主）** | 平台 → Agent `POST /execute` | 评测任务逐用例执行 | 是 |
| **上报（补充）** | Agent → 平台 `POST /api/trace` | 离线补录、调试、独立验证 | 否 |

两者共用同一份 Trace 数据结构（`contract` 模块中的 `TraceReport` / `SpanDto` / `SpanEvent`），避免契约漂移。

## 2. 拉取：平台调用 Agent

### 请求

`POST {agent.base-url}/execute`，`Content-Type: application/json`

```json
{
  "task_id": "uuid",
  "case_id": "uuid",
  "input": "用户指令",
  "fault_injection": {
    "type": "TOOL_ERROR | STEP_MISSING | OUTPUT_FORMAT_ERROR | TIMEOUT | INPUT_MISUNDERSTAND",
    "target_span": "0"
  }
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| `task_id` | 否 | 评测任务 ID；独立调用时为 `null` |
| `case_id` | 否 | 用例 ID，便于 Agent 侧日志与平台结果关联 |
| `input` | 是 | 用户指令原文 |
| `fault_injection` | 否 | 来自用例配置；**普通 Agent 必须忽略整个字段**，不能因未知字段报错 |

`fault_injection.type` 取值：

| 类型 | Agent 侧行为 | 期望的归因分层 |
|------|--------------|----------------|
| `TOOL_ERROR` | 目标工具 Span 的 `error` 非空、`output` 为 null，追加 `ERROR` 事件 | 工具调用错误 |
| `STEP_MISSING` | 跳过工具链中间一步（`summarize` 仍执行） | 规划错误 |
| `OUTPUT_FORMAT_ERROR` | 返回截断/不合法的 JSON | 输出生成错误 |
| `TIMEOUT` | 阻塞超过平台超时时间 | 数据/环境问题 |
| `INPUT_MISUNDERSTAND` | 第一个 `LLM_CALL` 的计划与输入语义无关，调用无关工具 | 输入理解错误 |

`target_span` 为目标工具调用的序号（从 0 开始的执行顺序，参考实现中 `0=web_search`、`1=extract`、`2=summarize`），`null` 表示按类型默认位置。序号越界或不是数字时 Agent 返回 HTTP 400，**不静默回落到第一个工具**——静默回落会让平台侧归因与预期故障对不上。

### 响应

HTTP 200，`Content-Type: application/json`，body 为完整 `TraceReport`（见第 4 节）。

### 平台侧处理规则

| 情形 | 处理 |
|------|------|
| 超时（默认 60s） | 该用例 FAIL，归因「数据/环境问题」 |
| 连接失败 | 该用例 FAIL，归因「数据/环境问题」 |
| HTTP 非 2xx / body 不合契约 | 该用例 FAIL，归因 `UNKNOWN` |
| 返回的 `trace_id` 已存在 | 该用例 FAIL，归因 `UNKNOWN` |
| 其余 | 正常入库、评分、归因 |

- **不重试**：一次调用对应一次结果，保证可复现。
- 失败也计入通过率分母，不跳过用例。

## 3. 上报：Agent 上报 Trace

`POST /api/trace`，body 为完整 `TraceReport`（见第 4 节）。

```json
{"code":0,"message":"ok","data":{"traceId":"...","duplicated":false}}
```

- `trace_id` 是幂等键：已存在时不覆盖、不合并，返回 `duplicated: true`。
- 上报的 Trace `source = REPORT`，不进入评测结果。
- 结构校验失败返回 HTTP 400，`code = 40002`，`message` 列出全部违规项。

### 查询

`GET /api/trace/{traceId}` → `TraceDetail`，`spans` 按 `start_time` 升序展平。不存在返回 HTTP 404（`code = 40401`）。

## 4. TraceReport 结构

```json
{
  "trace_id": "uuid",
  "agent_name": "fault-injection-agent",
  "status": "SUCCESS | FAILED | TIMEOUT",
  "output": "Agent 最终输出",
  "total_tokens": 1234,
  "duration_ms": 5678,
  "spans": [
    {
      "span_id": "uuid",
      "parent_span_id": "uuid | null",
      "span_type": "LLM_CALL | TOOL_CALL | RETRIEVAL",
      "tool_name": "工具名 | null",
      "input": "输入参数",
      "output": "输出结果",
      "error": "错误信息 | null",
      "tokens": 100,
      "duration_ms": 234,
      "start_time": "2026-10-26T10:00:00.000Z",
      "end_time": "2026-10-26T10:00:00.234Z",
      "events": [
        {
          "event_type": "LOG | WARNING | ERROR",
          "message": "日志内容",
          "timestamp": "2026-10-26T10:00:00.234Z"
        }
      ]
    }
  ]
}
```

### 字段约束

| 字段 | 约束 |
|------|------|
| `trace_id` | 非空，≤64 字符，全局唯一 |
| `agent_name` | 非空，≤128 字符 |
| `status` | 非空 |
| `spans` | 非空（至少 1 条），≤1000 条 |
| `span_id` | 非空，同一 Trace 内唯一，≤64 字符 |
| `parent_span_id` | ≤64 字符；为空或指向同一 Trace 内存在的 `span_id`；不能指向自身；父子关系不能成环 |
| `span_type` | 非空；`TOOL_CALL` 时 `tool_name` 必填（≤128 字符） |
| `start_time` / `end_time` | ISO 8601 带时区；`end_time` 不早于 `start_time` |
| `tokens` / `duration_ms` | 缺省按 0 处理 |

时间统一 ISO 8601（`2026-10-26T10:00:00.000Z`），不使用 `yyyy-MM-dd HH:mm:ss`。

## 5. 故障注入 Agent

阶段 1 提供的参考实现，模块 `agents/demo`，构建产物 `fault-injection-agent.jar`。

### 运行

```bash
java -jar agents/demo/target/fault-injection-agent.jar
```

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| `EVALLAB_AGENT_PORT` | `8081` | 监听端口 |
| `EVALLAB_AGENT_TIMEOUT_DELAY` | `65s` | `TIMEOUT` 故障阻塞时长，需大于平台超时 |
| `EVALLAB_SELF_REPORT` | `true` | 启动后自动上报 1 个正常 + 5 个故障 Trace |
| `EVALLAB_PLATFORM_URL` | `http://localhost:8080` | 自检上报地址 |

### 模拟工作流

固定两步工具链，便于确定性归因：

```text
LLM_CALL(规划)
└── TOOL_CALL(web_search)
    └── TOOL_CALL(extract)
        └── TOOL_CALL(summarize)
```

每种故障只改动一处结构：

| 故障 | 改动 |
|------|------|
| `TOOL_ERROR` | 指定工具 Span 的 `error` 非空、`output` 为空、追加 `ERROR` 事件；`status = FAILED` |
| `STEP_MISSING` | 缺少 `extract` 一步，`output` 标注不完整；`status = FAILED` |
| `OUTPUT_FORMAT_ERROR` | `output` 为截断的 JSON；`status = SUCCESS` |
| `TIMEOUT` | 阻塞后返回；`status = TIMEOUT` |
| `INPUT_MISUNDERSTAND` | 规划与输入语义无关、调用 `weather_query`；`status = SUCCESS` |

`OUTPUT_FORMAT_ERROR` 与 `INPUT_MISUNDERSTAND` 故意保持 `status = SUCCESS`：错误体现在输出与工具序列上，由评分器判定，而不是靠 Agent 自报状态——这样平台不能只信 `status`。

## 6. 契约变更规则

- 新增字段必须可选，旧 Agent 不填也能通过校验。
- 字段重命名视为破坏性变更，需提升版本并在本文件记录。
- 校验规则放宽不设版本；收紧必须在此列出，并同步更新故障注入 Agent 与用例。
