# 🤖 AI 编程规则 — 智能客服 Agent 项目

> 本文件是给 AI 编程工具（Cursor、Copilot、Antigravity 等）看的项目级规则。
> 所有 AI 协助开发时必须遵守以下约束。

---

## 1. 项目概况

- **项目名称**: 智能客服 Agent 应用 (agent-app)
- **技术栈**: Java 21 + Spring Boot 3.4.4 + Spring AI 1.0 + Spring AI Alibaba 1.0.0.2
- **构建工具**: Maven (多模块)
- **包路径**: `com.agent`
- **模块**: `agent-server`（后端核心服务）

## 2. 代码规范

### 2.1 语言与编码
- 代码注释**必须使用中文**
- 日志消息使用中文（`log.info("用户请求对话, 会话ID: {}", id)`）
- 代码中的类名、方法名、变量名使用英文
- 文件编码统一 **UTF-8**

### 2.2 代码组织
```
com.agent
├── config/          # 配置类（@Configuration）
├── controller/      # REST API 控制器
├── service/         # 业务逻辑层
│   ├── chat/        # 对话相关服务
│   └── rag/         # RAG 知识库相关服务
├── model/
│   ├── dto/         # 数据传输对象
│   └── entity/      # 持久化实体（后续添加）
├── tools/           # Agent 工具集（@Tool 注解）
└── common/          # 通用工具和常量
```

### 2.3 Spring AI 约定
- 使用 `ChatClient` 作为唯一入口，禁止直接调用底层 `ChatModel`
- Advisor 链统一在 `AiConfig.chatClient()` 中配置
- Tool 类使用 `@Component` + `@Tool` 注解，禁止手动注册
- ChatMemory 使用 `MessageWindowChatMemory`，窗口大小通过配置控制
- VectorStore 当前为 `SimpleVectorStore`，后续迁移到 `PgVectorStore` 时只改 Bean 配置

### 2.4 API 设计
- 所有接口统一使用 `ApiResult<T>` 封装响应
- 异常统一由 `GlobalExceptionHandler` 处理
- 流式接口使用 `text/event-stream` + `Flux<String>`
- 路径前缀：`/api/chat`（对话）、`/api/knowledge`（知识库）

### 2.5 Lombok 使用
- DTO 使用 `@Data` + `@Builder`
- Service 使用 `@RequiredArgsConstructor` 构造器注入
- 日志使用 `@Slf4j`

## 3. 禁止事项

### 3.1 绝对禁止
- ❌ 不要引入 LangChain4j 或其他 AI 框架，本项目只使用 Spring AI
- ❌ 不要把 API Key 硬编码在代码或配置文件中，必须用环境变量 `${DASHSCOPE_API_KEY}`
- ❌ 不要删除或重构现有的 `@Tool` 注解方法签名（LLM 依赖这些描述做路由）
- ❌ 不要在 Controller 层写业务逻辑，必须下沉到 Service
- ❌ 不要使用 `new` 创建 Bean，必须通过 Spring IOC 注入

### 3.2 修改约束
- ⚠️ 修改 `AiConfig.java` 前必须确认不会破坏 ChatClient 链路
- ⚠️ 修改 `application.yml` 的 AI 配置前需同步更新 `application-dev.yml`
- ⚠️ 新增 Tool 时必须写清楚中文 `description`，这是 LLM 选择工具的依据
- ⚠️ 修改 POM 依赖版本时检查 BOM 兼容性

## 4. 开发阶段感知

当前处于 **Phase 1（最小可运行底座）** 阶段：
- 优先保证编译通过和基本功能可用
- 允许使用 Mock 数据和内存存储
- 不要过度设计，不要提前引入生产级复杂度
- 具体进度参见 `PROJECT_STATUS.md`
- 下一步任务参见 `NEXT_ACTIONS.md`

## 5. 关键文件清单

| 文件 | 作用 | 修改敏感度 |
|------|------|-----------|
| `AiConfig.java` | AI 核心配置（ChatClient、Memory、VectorStore） | 🔴 高 |
| `ChatService.java` | 对话核心逻辑 | 🔴 高 |
| `CustomerServiceTools.java` | Agent 工具集 | 🟡 中 |
| `application.yml` | 全局配置 | 🟡 中 |
| `system-prompt.st` | 系统提示词模板 | 🟡 中 |
| `pom.xml` (root + server) | 依赖管理 | 🟡 中 |
| `docker-compose.yml` | 本地基础设施 | 🟢 低 |

## 6. 常用命令

```bash
# 编译
mvn clean compile

# 本地启动（开发环境）
cd agent-server && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 打包
mvn clean package -DskipTests

# 启动基础设施
docker-compose up -d

# 测试对话
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "你好"}'
```

## 7. 决策追溯

所有技术选型和架构决策记录在 `DECISIONS.md` 中，修改前务必查阅相关决策的背景和理由。

---

*本文件随项目阶段推进更新，当前版本适用于 Phase 1。*
