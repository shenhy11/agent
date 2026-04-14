## ADDED Requirements

### Requirement: 对话摘要压缩
系统 SHALL 在对话超出工作记忆窗口时，自动将早期对话压缩为摘要。

#### Scenario: 触发摘要压缩
- **WHEN** 对话消息数超过窗口大小（10 轮）
- **THEN** 系统用 LLM 将最早的 5 轮对话压缩为一段摘要文本，保留在上下文中

#### Scenario: 摘要内容质量
- **WHEN** 生成摘要
- **THEN** 摘要包含用户关注的产品型号、已讨论的关键点、用户偏好等核心信息

### Requirement: Redis 持久化
系统 SHALL 将对话记忆和摘要持久化到 Redis。

#### Scenario: 记忆保存
- **WHEN** 每轮对话完成后
- **THEN** 系统将当前工作记忆和摘要写入 Redis，key 为 conversationId，TTL 为 24 小时

#### Scenario: 记忆恢复
- **WHEN** 用户使用已有的 conversationId 发起对话
- **THEN** 系统从 Redis 恢复工作记忆和摘要，继续对话上下文

#### Scenario: 无 Redis 降级
- **WHEN** Redis 连接不可用
- **THEN** 系统降级为纯内存记忆模式（MessageWindowChatMemory），功能不受影响

### Requirement: 用户画像沉淀
系统 SHALL 从多次对话中提取用户偏好，形成长期画像。

#### Scenario: 提取偏好
- **WHEN** 用户在多轮对话中提到"我经常跑步""预算 2000 以内"
- **THEN** 系统将偏好标签（运动场景、预算约束）写入 Redis Hash

#### Scenario: 画像应用
- **WHEN** 用户发起新对话
- **THEN** 系统将用户画像注入系统提示词，个性化回答
