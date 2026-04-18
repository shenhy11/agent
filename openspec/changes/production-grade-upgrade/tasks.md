## 阶段一：安全加固与配置外部化

- [x] 1.1 敏感配置外部化：将 `application.yml` 中的 `dashscope.api-key` 改为 `${DASHSCOPE_API_KEY}` 环境变量引用，删除硬编码值
- [x] 1.2 多环境配置分离：创建 `application-dev.yml` / `application-prod.yml`，共用配置保留在 `application.yml`
- [x] 1.3 引入 Spring Security 依赖：`pom.xml` 新增 `spring-boot-starter-security` + `jjwt` (0.12.x)
- [x] 1.4 创建 `JwtUtil.java`：JWT Token 生成/解析/验证工具类，支持 Access (2h) 和 Refresh (7d) 双 Token
- [x] 1.5 创建 `sys_user` JPA Entity 和 Repository：`SysUser.java` / `SysUserRepository.java`
- [x] 1.6 创建 `AuthController.java`：`POST /api/v1/auth/login` 和 `POST /api/v1/auth/refresh` 端点
- [x] 1.7 创建 `SecurityConfig.java`：Spring Security 过滤链配置，放行 `/api/v1/auth/**`，其余强制 JWT 校验
- [x] 1.8 创建 `JwtAuthenticationFilter.java`：OncePerRequestFilter，从请求头提取并验证 JWT
- [x] 1.9 创建 `UserService.java`：用户注册（BCrypt 加密）/ 登录 / 查询
- [x] 1.10 创建 `AdminController.java`：`POST /api/v1/admin/users`、`GET /api/v1/admin/users` 管理端点（ADMIN 角色限制）
- [x] 1.11 所有现有 Controller 路径添加 `/api/v1/` 前缀
- [x] 1.12 创建 `GlobalExceptionHandler.java`：`@RestControllerAdvice` 全局异常处理
- [x] 1.13 创建 `ErrorCode.java` 枚举：按模块定义错误码（AUTH_001 / USER_001 / NLP_001 / VISION_001 / SYS_500）
- [x] 1.14 创建 `BusinessException.java`：自定义业务异常类，携带 ErrorCode
- [x] 1.15 验证：启动后调用 `/api/v1/auth/login` 获取 Token，携带 Token 访问 NLP 接口成功

## 阶段二：存储层升级

- [x] 2.1 `pom.xml` 新增 PostgreSQL 驱动 + Spring Data JPA + Flyway + spring-ai-pgvector-store 依赖
- [x] 2.2 `application.yml` 配置 PostgreSQL 数据源（`spring.datasource.*`）和 Flyway
- [x] 2.3 创建 Flyway 迁移脚本 `V1__init_schema.sql`：建 sys_user / chat_session / chat_message / audit_log 四张表
- [x] 2.4 创建 Flyway 迁移脚本 `V2__enable_pgvector.sql`：`CREATE EXTENSION IF NOT EXISTS vector`
- [x] 2.5 创建 `ChatSession` + `ChatMessage` JPA Entity 和 Repository
- [x] 2.6 创建 `AuditLog` JPA Entity 和 Repository
- [x] 2.7 创建 `AuditAspect.java`：AOP 切面自动记录 AI 接口调用审计日志
- [x] 2.8 修改 `AiConfig.java` 中的 VectorStore Bean：从 SimpleVectorStore 切换为 PgVectorStore
- [x] 2.9 修改 `ImageSearchService.java`：移除 `@PostConstruct` 内存初始化，改为启动时检测 pgvector 是否已有数据
- [x] 2.10 修改 `ChatService.java`：对话消息持久化至 chat_message 表
- [x] 2.11 创建 `SessionController.java`：`GET /api/v1/sessions` / `GET /api/v1/sessions/{id}/messages` 端点
- [x] 2.12 验证：重启应用后向量数据和用户数据仍存在（需 PostgreSQL 启动后联调验证）

## 阶段三：前端现代化 (React + Next.js 14)

- [x] 3.1 初始化 Next.js 14 项目：`npx create-next-app@latest agent-next --typescript --tailwind --app`，安装 Shadcn/ui / Zustand / axios
- [x] 3.2 配置 Shadcn/ui：`npx shadcn-ui@latest init`，选择 Dark 主题
- [x] 3.3 创建全局布局 `app/layout.tsx`：假边栏 + 顶部导航栏 + 主内容区
- [x] 3.4 创建 `lib/auth.ts`：登录/登出/Token 管理函数
- [x] 3.5 创建 `store/` 中的 Zustand Store：authStore / chatStore / nlpStore / visionStore
- [x] 3.6 创建 `lib/axios.ts`：axios 实例 + 请求拦截器 (JWT 注入) + 响应拦截器 (401 跳转)
- [x] 3.7 创建 `middleware.ts`：Next.js Middleware 路由守卫，未登录跳转 `/login`
- [x] 3.8 创建 `app/login/page.tsx`：登录页面 + Shadcn Form 表单
- [x] 3.9 创建 `app/page.tsx`：Dashboard 首页
- [ ] 3.10 创建 `app/ai-lab/page.tsx`：AI 能力展厅主页（迁移自 ai-lab.html）
- [ ] 3.11 创建 `app/ai-lab/chat/page.tsx`：智能客服对话页（迁移自 demo-chat.html）
- [ ] 3.12 创建 `app/ai-lab/nlp/page.tsx`：NLP 文本分析页（迁移自 demo-nlp.html，含 8 个 Tab）
- [ ] 3.13 创建 `app/ai-lab/vision/page.tsx`：多模态图文页（迁移自 demo-vision.html，含 5 个 Tab）
- [ ] 3.14 创建 `app/ai-lab/rag/page.tsx`：RAG 知识库页（迁移自 demo-rag.html）
- [ ] 3.15 创建 `app/ai-lab/agent/page.tsx`：Agent 编排页（迁移自 demo-agent.html）
- [ ] 3.16 创建 `app/admin/users/page.tsx`：用户管理页（ADMIN）
- [ ] 3.17 创建 `app/admin/audit/page.tsx`：审计日志页（ADMIN）
- [ ] 3.18 验证：所有页面功能与原 HTML 版保持一致

## 阶段四：可观测性与部署

- [ ] 4.1 `pom.xml` 新增 `spring-boot-starter-actuator` + `micrometer-registry-prometheus` 依赖
- [ ] 4.2 配置 `application.yml`：暴露 `/actuator/health` 和 `/actuator/prometheus` 端点
- [ ] 4.3 Logback 配置改为 JSON 格式输出（`logback-spring.xml` + `logstash-logback-encoder`）
- [ ] 4.4 创建 API 限流拦截器：基于 Bucket4j 或 Guava RateLimiter，NLP/Vision 接口每分钟 30 次限制
- [ ] 4.5 pom.xml 新增 springdoc-openapi 依赖，配置 Swagger UI 路径
- [ ] 4.6 更新 `docker/Dockerfile.backend`：多阶段构建 Spring Boot JAR
- [ ] 4.7 创建 `docker/Dockerfile.frontend`：Next.js 构建产物 + Nginx 静态托管
- [ ] 4.8 更新 `docker-compose.yml`：三容器编排（Spring Boot + PostgreSQL + Nginx）
- [ ] 4.9 创建 `docker/nginx.conf`：反向代理 + HTTPS 配置 + gzip
- [ ] 4.10 创建 `docker/.env.example`：环境变量模板（DASHSCOPE_API_KEY / DB_PASSWORD / JWT_SECRET）
- [ ] 4.11 验证：`docker compose up -d` 一键启动，所有功能正常
