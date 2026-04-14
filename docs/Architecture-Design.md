# 智能客服系统架构设计文档

## 1. 技术栈选型
| 分层 | 技术组件 | 备注 |
|---|---|---|
| **语言运行时** | Java 21 | 启用虚拟线程应对高并发流式输出 |
| **基础框架** | Spring Boot 3.4.x | |
| **AI 编排底座** | Spring AI 1.0 GA | + Spring AI Alibaba 核心依赖 |
| **模型推理服务** | 阿里云通义千问 (DashScope) | 备用：OpenAI 等兼容模型 |
| **向量数据库** | PostgreSQL 16 + pgvector | 存储高维 Embedding 向量数据 |
| **缓存与会话** | Redis 7 | 存储聊天历史窗口与高频问题 (Q&A) |

## 2. 逻辑架构图
```text
[ 前端应用 (Web/H5/App) ]  <-- SSE Stream --> [ 智能网关/负载均衡 (阿里云 SLB) ]
                                             |
+--------------------------------------------|---------------------------------------------+
|                               Agent 核心应用服务 (ECS / ACK)                             |
|                                                                                          |
|  [ API 接入层 ] : /api/chat (同步) | /api/chat/stream (流式) | /api/knowledge (知识库) |
|                                                                                          |
|  [ Agent 编排层 ]:                                                                       |
|      +-- ChatClient Pipeline                                                             |
|          |-- Prompt Template (人格设定)                                                  |
|          |-- MessageChatMemoryAdvisor (历史记忆注入)                                     |
|          |-- QuestionAnswerAdvisor (RAG 检索注入)                                        |
|          +-- Tool Calling Interceptor (工具路由与调用截击)                               |
|                                                                                          |
|  [ 工具支持层 ]:                                        [ 知识工程层 ]:                  |
|      - OrderTool (订单)                                  - DocumentReader (Tika)         |
|      - RefundTool (退款)                                 - TokenTextSplitter             |
|      - TicketTool (工单)                                 - EmbeddingModel (通义向量)      |
+------------------------------------------------------------------------------------------+
       |                                      |                                  |
[ 内部业务微服务 集群 ]                 [ PostgreSQL (pgvector) ]              [ Redis 集群 ]
```

## 3. RAG 知识库检索全链路设计
1. **文档摄入 (Ingestion)**：
   - 用户 / 管理员调用 `/api/knowledge/upload` 接口上传私有增强文档（PDF/Word）。
   - Tika 工具执行文本提取。
   - `TokenTextSplitter` 按照每块 1000 Tokens、重叠 200 Tokens 进行切分。
2. **向量化 (Embedding)**：
   - 切片文本传递给 `DashScopeEmbeddingModel` 计算高维向量数组。
   - 数据结合 Metadata 持久化至 PostgreSQL `vector_store` 表里。
3. **检索增强 (Retrieval)**：
   - 收到用户请求时，`QuestionAnswerAdvisor` 从请求提取问题语义并向量化。
   - 发起 PG 相似度检索 (Cosine Similarity)，取 Top-4 相关分块。
   - 作为 `context` 变量置入 System Prompt 再抛给大模型。

## 4. 对话记忆管理设计
为了防止大模型丢失上下文轮次，采用 **MessageWindowChatMemory** 策略：
- 默认保留最近 15 条互动（或者最近 X 万个 Token）。
- 通过 `ChatMemory.CONVERSATION_ID` 以 UUID 在 Redis（或内存）中开辟独立空间。
- 在构建 Prompt 前，`MessageChatMemoryAdvisor` 自动将 Redis 中的历史对话拼接为 Messages 链推给 LLM。

## 5. Tool Calling (工具调用) 契约设计
- 将 Java Service 方法打上 `@Tool(description="...")` 与参数 `@ToolParam(...)`。
- Spring AI 反射提取方法特征，转换为 JSON Schema 发给大模型。
- LLM 返回需要执行的方法名及提取到的 JSON 参数。
- 本地框架内部调用业务接口，并将执行结果拼接回 Message，发起第二次 LLM 汇总生成，最终推给用户端。
