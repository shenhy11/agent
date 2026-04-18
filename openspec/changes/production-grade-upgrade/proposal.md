## Why

ChronoTech AI Lab 当前处于 Demo 级，存在 API-Key 硬编码、无认证鉴权、向量数据重启丢失、无持久化数据库等致命生产级缺口。需要将其改造为可在阿里云上稳定运行的企业内部 AI 能力展示平台，面向企业内部用户提供安全可靠的 NLP、多模态、RAG、Agent 等 AI 能力服务。

## What Changes

### 阶段一：安全加固与配置外部化
- 所有 API-Key / 敏感配置迁移至环境变量，禁止明文硬编码
- 引入 Spring Security + JWT 认证鉴权体系（登录/注册/角色控制）
- 多环境配置分离（`application-dev.yml` / `application-prod.yml`）
- 全局异常处理 + 统一错误码体系
- API 路径版本化：`/api/v1/*`

### 阶段二：存储层升级
- 引入 PostgreSQL 数据库（用户表、会话表、审计日志表）
- 向量存储从 `SimpleVectorStore`（内存）迁移至 `pgvector`（PostgreSQL 扩展）
- Flyway 数据库版本迁移管理
- 知识库文档持久化存储

### 阶段三：前端现代化
- 前端从纯静态 HTML 重写为 React (Next.js) SPA 应用
- 组件化设计 + Zustand 状态管理 + Next.js App Router
- Shadcn/ui + Tailwind CSS 组件库 + 暗色主题
- API 层统一封装（axios 拦截器 + JWT 自动注入）
- 登录页 + 中间件路由守卫（Next.js Middleware）

### 阶段四：可观测性与部署
- 结构化日志（Logback JSON）+ Micrometer 指标暴露
- Docker Compose 生产配置（Spring Boot + PostgreSQL + Nginx）
- 阿里云 ECS 部署方案 + HTTPS 证书
- API 限流（Bucket4j / Resilience4j）
- Token 消耗计量与日志审计

## Capabilities

### New Capabilities
- `security-auth-jwt`: JWT 认证鉴权体系 — 用户注册/登录、Token 签发刷新、RBAC 角色权限控制、Spring Security 过滤链配置
- `database-persistence`: PostgreSQL 持久化层 — 用户/会话/审计表设计、JPA Entity、Flyway 迁移脚本、事务管理
- `pgvector-storage`: pgvector 向量存储 — 从 SimpleVectorStore 迁移至 pgvector、知识库持久化、向量索引优化
- `nextjs-frontend`: React (Next.js 14) 前端应用 — App Router 组件化重写所有页面、登录鉴权流程（Next.js Middleware）、API 层封装、暗色主题（Shadcn/ui + Tailwind CSS）
- `api-standardization`: API 标准化 — 版本化路径、全局异常处理、统一响应格式、OpenAPI 文档生成
- `observability-monitoring`: 可观测性 — 结构化日志、Prometheus 指标、Docker 生产部署、限流防刷

### Modified Capabilities
- `rag-advanced-retrieval`: 向量存储后端从 SimpleVectorStore 切换为 pgvector，检索配置需适配新存储
- `agent-memory-system`: 对话记忆从内存切换为数据库持久化存储

## Impact

- **后端代码**：新增 Spring Security 配置、JPA Entity、Flyway 迁移、全局异常处理器。所有 Controller 路径加 `/api/v1/` 前缀，`application.yml` 拆分为多环境
- **前端代码**：`agent-web/` 目录下纯 HTML 将被全新的 Nuxt 项目替代，原有 JS 逻辑迁移至 Vue 组件
- **基础设施**：新增 PostgreSQL + pgvector 依赖，Docker Compose 需新增 PG 服务容器
- **API 契约**：所有端点路径变更为 `/api/v1/*`，请求需携带 `Authorization: Bearer <token>` 头 **BREAKING**
- **部署方式**：从 `start.bat` 手动启动变更为 Docker Compose 一键部署
