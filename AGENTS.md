

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
feat: 实现 Trace 采集接口与 span 父子关系解析
fix: 修复工具调用评分器在工具重复调用时序列比对错位
```

