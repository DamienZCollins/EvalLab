

## git 相关

commit message 格式：`<type>: <精炼描述>`，不堆砌内容。

每次提交控制在 50 行左右，便于 review；搭建骨架、文档、引入依赖等无法拆分的提交除外。

type 取值：

- `feat`：新增功能
- `fix`：修复 bug
- `refactor`：重构，不改变外部行为
- `perf`：性能优化
- `docs`：文档
- `test`：测试
- `build`：构建、依赖
- `chore`：杂项，如配置、脚本

示例：

```
feat: 秒杀下单接口接入 RocketMQ 异步创建订单
fix: 修复库存回补时未校验订单状态导致的重复回补
```

