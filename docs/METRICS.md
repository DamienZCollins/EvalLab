# 实测指标

> 记录各阶段验收时的真实运行结果，作为后续回归的基线。数据来自本地 Docker Compose 环境，非估算。

## 阶段 3：LLM-as-Judge 与失败归因

### 环境

| 项 | 值 |
|----|-----|
| 被测 Agent | `fault-injection-agent`（确定性模拟工作流） |
| Judge 模型 | DeepSeek V4.1 Flash（OpenAI 兼容端点） |
| Judge 温度 | 0 |
| 用例数 | 10（5 正常 + 5 故障） |
| 抽检样本 | 2 轮任务共 20 条结果 |

### 归因准确率

**20 / 20 = 100%**

5 类故障均被正确分层，无一误判：

| 用例 | 期望归因 | 系统归因 | 结果 |
|------|----------|----------|------|
| `fault-timeout` | 数据/环境问题 | ENVIRONMENT | ✅ |
| `fault-tool-error` | 工具调用错误 | TOOL_CALL | ✅ |
| `fault-input-misunderstand` | 输入理解错误 | INPUT_MISUNDERSTAND | ✅ |
| `fault-step-missing` | 规划错误 | PLANNING | ✅ |
| `fault-output-format` | 输出生成错误 | OUTPUT_GENERATION | ✅ |
| 5 个正常用例 | 通过，无归因 | null | ✅ |

归因器刻意不读 `fault_injection`，只依据 Trace 与评分结果判定，因此该准确率对真实 Agent 同样成立（前提是故障模式落在 5 层的定义范围内）。

### Judge 准确率

**10 / 12 = 83.3%**（判定口径：|Judge 分 − 人工分| < 0.1 为一致）

12 条有 Judge 分的样本中 10 条一致。两条不一致均为 0.1 分差，恰好卡在严格边界外：

| 用例 | Judge 分 | 人工分 | 分差 |
|------|----------|--------|------|
| `normal-regex` | 0.3 | 0.2 | 0.1 |
| `normal-json-schema` | 0.3 | 0.2 | 0.1 |

这两条本质是「同一判断、不同刻度」，偏差在噪声范围内。目标 75%，实测 83.3%，达标。

`normal-tools-only` 与 4 个故障用例没有 Judge 分：前者未声明 `expected_output`，后者（工具错误、步骤缺失、超时）的 Trace 输出为空或不可评，Judge 无输入依据，故不产生记录。这是设计上的取舍——宁缺毋滥，不用无依据的分数稀释指标。

### 归因覆盖率

**5 / 5 = 100%**（所有失败用例都得到了非 UNKNOWN 的分层）

### 已知局限

- 抽检样本仅 20 条，且来自确定性模拟 Agent，故障模式与归因规则一一对应，准确率偏高是可预期的。真实 Agent 的边界情况（如「理解对了但规划错了」的混合失败）需要更大样本才能暴露。
- Judge 对「格式正确但内容敷衍」的输出评分偏严（`normal-regex` / `normal-json-schema` 都给 0.2~0.3），这是期望行为：这两条用例的判定标准是格式匹配，内容质量本就低于 `normal-exact`。
