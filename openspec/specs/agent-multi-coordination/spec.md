## ADDED Requirements

### Requirement: Supervisor 路由
系统 SHALL 实现 Supervisor Agent 模式，根据用户意图将请求路由到对应的专家 Agent。

#### Scenario: 意图识别与路由
- **WHEN** 用户发送消息
- **THEN** Supervisor 用 LLM 判断意图类别（客服咨询/数据分析/知识检索），路由到对应子 Agent

#### Scenario: 路由回退
- **WHEN** Supervisor 无法明确判断意图
- **THEN** 默认路由到客服 Agent

### Requirement: 专家 Agent 分工
系统 SHALL 配置至少 3 个专家 Agent，各有独立的系统提示词和工具集。

#### Scenario: 客服 Agent
- **WHEN** 用户咨询产品信息、售后问题
- **THEN** 客服 Agent 使用 ProductTools + CustomerServiceTools 回答

#### Scenario: 分析 Agent
- **WHEN** 用户要求对比分析、推荐决策
- **THEN** 分析 Agent 使用 compareProducts + 推理能力给出结构化分析报告

#### Scenario: 检索 Agent
- **WHEN** 用户明确查询知识库内容
- **THEN** 检索 Agent 通过 RAG 管道检索并返回带来源引用的答案

### Requirement: Agent 间结果传递
系统 SHALL 支持 Supervisor 在需要时组合多个子 Agent 的结果。

#### Scenario: 多 Agent 组合回答
- **WHEN** 用户问题跨越多个 Agent 职能（如"推荐一款适合跑步的手表并告诉我保修政策"）
- **THEN** Supervisor 分别调用客服 Agent 和检索 Agent，合并结果回答
