## ADDED Requirements

### Requirement: Agent 思考链可视化
系统 SHALL 在 SSE 流式响应中暴露 Agent 的完整思考过程，包括推理步骤、工具调用决策、工具执行结果。

#### Scenario: Agent 调用工具时暴露过程
- **WHEN** 用户问 "帮我对比 Sport X2 Pro 和 Elite E2 Ultra"
- **THEN** SSE 流依次推送 thinking、tool_call、tool_result、text 类型事件，前端可渲染为步骤链

### Requirement: 产品查询工具
系统 SHALL 提供 `@Tool` 注解的产品查询方法，Agent 可在对话中自主调用以获取产品规格数据。

#### Scenario: Agent 自主查询产品
- **WHEN** 用户问 "Sport X2 Pro 的续航多长？"
- **THEN** Agent 判断需要调用 queryProduct 工具，获取规格后用自然语言回答

### Requirement: 产品对比工具
系统 SHALL 提供 `@Tool` 注解的产品对比方法，Agent 可对比多款产品的规格差异。

#### Scenario: Agent 自主执行多步对比
- **WHEN** 用户问 "运动系列和商务系列有什么区别？"
- **THEN** Agent 分别查询两个系列的产品，对比后生成总结

### Requirement: 产品推荐工具
系统 SHALL 提供 `@Tool` 注解的产品推荐方法，根据用户描述的需求推荐最适合的产品。

#### Scenario: 根据场景推荐
- **WHEN** 用户问 "我经常跑步和游泳，推荐哪款？"
- **THEN** Agent 调用推荐工具，基于防水和运动功能推荐合适产品

### Requirement: 手表场景 System Prompt
系统 SHALL 使用专为 ChronoTech 手表场景定制的 system prompt，包含品牌信息、产品线概述、服务规范。

#### Scenario: AI 具备品牌认知
- **WHEN** 用户问 "你们公司是做什么的？"
- **THEN** AI 回答 ChronoTech 品牌信息，而非通用回答

### Requirement: Agent 多步推理循环
系统 SHALL 支持 Agent 在单次对话中进行多步推理 — 即 LLM 可连续调用多个工具，观察每步结果后决定下一步操作。

#### Scenario: 复杂任务多步执行
- **WHEN** 用户要求 "对比 Sport X2 Pro 和 Rugged R2 Titan，帮我分析哪个适合户外徒步，并查下最近有没有优惠"
- **THEN** Agent 依次执行：查询产品A → 查询产品B → 查询优惠 → 综合分析 → 生成报告
