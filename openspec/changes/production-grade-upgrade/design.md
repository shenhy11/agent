## Context

ChronoTech AI Lab 是一个基于 Spring Boot 3.4 + Spring AI 的企业级 AI 能力展示平台，当前技术栈：
- **后端**：Spring Boot 3.4 单体 (Java 21) + Spring AI Alibaba (DashScope 通义千问) + Redis + Lucene BM25 + SimpleVectorStore (内存)
- **前端**：纯静态 HTML × 11 页 + Vanilla JS/CSS，通过 `python http.server` 托管
- **依赖**：Lombok / Hutool / Tika 文档解析 / Spring AI Tika Reader
- **部署**：`start.bat` 手动启动，无 CI/CD

关键缺口：API-Key 明文硬编码、无认证鉴权、向量数据内存存储、无关系数据库、前端无构建工具链。目标是在保留现有 AI 能力（NLP/多模态/RAG/Agent）的基础上，补全生产级基础设施。

## Goals / Non-Goals

**Goals:**
- 安全：杜绝 API-Key 泄露风险，建立 JWT 认证鉴权体系
- 持久化：引入 PostgreSQL + pgvector 替代内存存储，数据重启不丢失
- 前端现代化：Vue 3 (Nuxt 3) SPA 应用替代纯静态 HTML
- 可部署：Docker Compose 一键部署到阿里云 ECS
- 可观测：结构化日志 + 基础指标监控

**Non-Goals:**
- 不做多租户隔离（当前仅面向企业内部单组织）
- 不做 Token 付费计量系统（内部使用无计费需求）
- 不做微服务拆分（单体足以支撑当前规模）
- 不做 K8s 编排（Docker Compose 足够）
- 不做前端 SSR（Nuxt SPA 模式即可，不需要 SEO）

## Decisions

### D1. 认证方案：Spring Security + JWT (自签发)

**选择**：Spring Security 6 + 自签 JWT（HMAC-SHA256），不依赖外部 IdP。

**备选方案**：
- OAuth2 + Keycloak → 过重，企业内部场景不需要
- Session + Cookie → 不适合前后端分离 SPA 架构

**理由**：JWT 无状态、前后端解耦，且 Spring Security 6 对 JWT 支持成熟。企业内部用户量小（<100），不需要外部认证服务。Token 过期时间设 2 小时，配合 Refresh Token 机制。

### D2. 数据库：PostgreSQL 16 + pgvector 扩展

**选择**：单个 PostgreSQL 16 实例同时承载关系数据和向量检索。

**备选方案**：
- PostgreSQL + 独立 Milvus → 额外运维成本，当前数据量不需要
- MySQL + 独立 Qdrant → 两套存储，复杂度翻倍

**理由**：pgvector 在百万级以下向量检索性能足够，与 Spring AI 官方集成良好。一个 PG 实例解决所有存储需求，运维成本最低。

### D3. ORM 方案：Spring Data JPA + Flyway

**选择**：JPA Entity + Flyway 迁移管理。

**理由**：团队已使用 Spring Boot 生态，JPA 学习成本为零。Flyway 确保数据库 schema 版本可追踪，支持回滚。

### D4. 前端框架：Next.js 14 (App Router) + Shadcn/ui + Tailwind CSS

**选择**：Next.js 14 App Router + React 18 + Shadcn/ui + Tailwind CSS + Zustand + Dark Mode。

**备选方案**：
- Nuxt 3 (Vue) → 用户最终选择 React 生态
- 纯 HTML 加固 → 无法实现组件化复用和路由守卫

**理由**：Next.js 14 是目前 AI 产品前端的行业主流选择（OpenAI、Anthropic 均使用 Next.js），App Router 提供了基于文件系统的路由 + Server Components。Shadcn/ui 是高质量的无头组件库，与 Tailwind CSS 搭配可实现极简设计感。Zustand 轻量易用，适合中小规模应用的状态管理。

### D5. API 标准化：版本化 + 全局异常 + OpenAPI

**选择**：
- 路径前缀统一为 `/api/v1/*`
- `@RestControllerAdvice` 全局异常处理
- `ApiResult<T>` 统一响应包装（已有基础，需补全错误码枚举）
- SpringDoc (OpenAPI 3.0) 自动文档生成

### D6. 部署方案：Docker Compose (3 容器)

```
┌─────────────────────────────────────────────────┐
│              Docker Compose Stack                │
│                                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────────┐  │
│  │  Nginx   │  │ Spring   │  │ PostgreSQL   │  │
│  │  :80/443 │──│ Boot     │──│ + pgvector   │  │
│  │  前端+   │  │ :8080    │  │ :5432        │  │
│  │  反向代理│  │          │  │              │  │
│  └──────────┘  └──────────┘  └──────────────┘  │
│       ↑               ↑              ↑          │
│    Nuxt 构建       JAR 包      数据卷持久化     │
│    静态产物                                      │
└─────────────────────────────────────────────────┘
```

**阿里云 ECS 最低配置建议**：2C4G + 40G SSD。

## 数据模型设计

```sql
-- 用户表
CREATE TABLE sys_user (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,  -- BCrypt
    nickname    VARCHAR(100),
    role        VARCHAR(20) DEFAULT 'USER',  -- USER / ADMIN
    enabled     BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW()
);

-- 会话表
CREATE TABLE chat_session (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT REFERENCES sys_user(id),
    title       VARCHAR(200),
    module      VARCHAR(50),  -- chat / nlp / vision / rag / agent
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW()
);

-- 对话消息表
CREATE TABLE chat_message (
    id          BIGSERIAL PRIMARY KEY,
    session_id  BIGINT REFERENCES chat_session(id),
    role        VARCHAR(20),  -- user / assistant / system
    content     TEXT,
    token_count INT,
    created_at  TIMESTAMP DEFAULT NOW()
);

-- 操作审计日志
CREATE TABLE audit_log (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT,
    action      VARCHAR(100),
    module      VARCHAR(50),
    detail      TEXT,
    ip          VARCHAR(45),
    created_at  TIMESTAMP DEFAULT NOW()
);
```

## 前端路由结构

```
/login                → 登录页
/                     → 首页 (Dashboard)
/ai-lab               → AI 能力展厅
/ai-lab/chat          → 智能客服对话
/ai-lab/nlp           → NLP 文本分析
/ai-lab/vision        → 多模态图文
/ai-lab/rag           → RAG 知识库
/ai-lab/agent         → Agent 编排
/admin/users          → 用户管理 (ADMIN)
/admin/audit          → 审计日志 (ADMIN)
```

## Risks / Trade-offs

| 风险 | 严重度 | 缓解措施 |
|------|--------|---------|
| pgvector 迁移后检索质量变化 | 中 | 迁移前后做 A/B 对比测试，保留 SimpleVectorStore 作为降级方案 |
| 前端全部重写工作量大 (11 页) | 高 | 分批迁移：先做 Login + 首页 + Chat，后续逐一迁移其他页面 |
| JWT Secret 泄露 | 高 | 环境变量注入，不同环境不同密钥，定期轮换 |
| 阿里云 DashScope 网络延迟 | 低 | 已部署在阿里云，延迟 < 100ms |
| Nuxt 3 构建产物较大 | 低 | 启用 gzip + 代码分割，Nginx 静态缓存 |

## Migration Plan

1. **数据库先行**：先部署 PostgreSQL + pgvector，创建表结构，旧数据通过 Flyway 初始化
2. **后端加固**：加入 Spring Security + JWT，旧端点加 `/api/v1/` 前缀，保持向后兼容
3. **存储切换**：VectorStore Bean 从 SimpleVectorStore 切换为 PgVectorStore
4. **前端重写**：Nuxt 项目独立构建，Nginx 反向代理，与后端并行开发
5. **部署上线**：Docker Compose 整合打包，部署至阿里云 ECS

**回滚策略**：每个阶段保留独立 Git 分支，数据库迁移通过 Flyway 的 undo 机制回退。

## Open Questions

1. 是否需要 LDAP/AD 对接企业统一身份认证？（当前方案为独立用户表）
2. 前端是否需要国际化（i18n）支持？
3. 审计日志需要保留多长时间？是否需要导出功能？
