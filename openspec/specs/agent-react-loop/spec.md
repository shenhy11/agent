## ADDED Requirements

### Requirement: ReAct 推理循环
系统 SHALL 实现显式的 ReAct（Reason-Act-Observe）循环，Agent 在每一步自主决定是否继续推理。

#### Scenario: 多步推理
- **WHEN** 用户提问"对比 Sport 系列两款手表并推荐"
- **THEN** Agent 执行多步循环：思考需要查哪些产品→调用 queryProduct→思考需要对比→调用 compareProducts→综合回答

#### Scenario: 自主终止
- **WHEN** Agent 判断已获得足够信息
- **THEN** Agent 输出最终回答并终止循环

#### Scenario: 最大步数限制
- **WHEN** 推理循环达到最大步数（10）
- **THEN** 系统强制终止并基于已有信息生成回答

### Requirement: 思考链 SSE 流式输出
系统 SHALL 通过 SSE 实时向前端推送每一步的推理过程。

#### Scenario: 推送思考事件
- **WHEN** Agent 进入思考阶段
- **THEN** 系统发送 `event: thinking` 包含步骤号和思考内容

#### Scenario: 推送工具调用事件
- **WHEN** Agent 决定调用工具
- **THEN** 系统依次发送 `event: tool_call`（工具名+参数）和 `event: tool_result`（返回结果+耗时）

### Requirement: 工具调用错误恢复
系统 SHALL 在工具调用失败时尝试恢复而非直接终止。

#### Scenario: 工具调用超时
- **WHEN** 工具调用超过 30 秒未返回
- **THEN** Agent 收到超时提示，自主决定是否重试或使用其他工具
