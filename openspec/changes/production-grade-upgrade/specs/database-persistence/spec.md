## ADDED Requirements

### Requirement: 用户数据持久化
系统 SHALL 使用 PostgreSQL 存储用户（sys_user）、会话（chat_session）、消息（chat_message）和审计日志（audit_log）。

#### Scenario: 用户注册后持久化
- **WHEN** 创建一个新用户并重启应用
- **THEN** 用户数据仍然存在，可正常登录

### Requirement: 会话与消息持久化
系统 SHALL 将对话会话和消息持久化至数据库，支持历史查询。

#### Scenario: 查询历史会话
- **WHEN** 用户调用 `GET /api/v1/sessions`
- **THEN** 返回该用户的所有历史会话列表（按更新时间倒序）

#### Scenario: 查询会话消息
- **WHEN** 用户调用 `GET /api/v1/sessions/{id}/messages`
- **THEN** 返回该会话中的所有消息（按时间正序）

### Requirement: 审计日志自动记录
系统 SHALL 对关键操作（登录/调用 AI 接口/管理操作）自动记录审计日志。

#### Scenario: AI 接口调用审计
- **WHEN** 用户调用 NLP/Vision/RAG 等 AI 接口
- **THEN** audit_log 表记录 user_id、action、module、IP、时间

### Requirement: Flyway 数据库迁移
系统 SHALL 使用 Flyway 管理数据库 Schema 版本，启动时自动执行迁移。

#### Scenario: 首次启动自动建表
- **WHEN** 应用首次连接空数据库启动
- **THEN** Flyway 自动执行所有迁移脚本 (V1__init.sql)，创建完整表结构
