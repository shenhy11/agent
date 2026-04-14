# 智能客服 Agent 应用

基于 **Spring AI + Spring AI Alibaba** 的企业级智能客服系统。

## 技术栈

- Java 21 + Spring Boot 3.4
- Spring AI 1.0 + Spring AI Alibaba 1.0
- PostgreSQL 16 + pgvector（向量存储）
- Redis 7（缓存/会话管理）
- React 18 + TypeScript（前端，开发中）

## 快速开始

### 1. 启动基础设施

```bash
docker-compose up -d
```

### 2. 配置 API Key

设置环境变量：

```bash
# 通义千问 API Key（必需）
export DASHSCOPE_API_KEY=your-dashscope-api-key

# OpenAI API Key（可选，备用模型）
export OPENAI_API_KEY=your-openai-api-key
```

### 3. 启动应用

```bash
cd agent-server
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 4. 测试对话 API

```bash
# 同步对话
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "你好，请问怎么查询订单状态？"}'

# 流式对话（SSE）
curl -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"message": "你好，请问怎么查询订单状态？"}'
```

## 项目结构

```
agent-app/
├── agent-server/          # 后端服务
│   ├── src/main/java/com/agent/
│   │   ├── config/        # 配置类（AI、Web、异常处理）
│   │   ├── controller/    # API 控制器
│   │   ├── service/       # 业务逻辑
│   │   ├── model/         # 数据模型（DTO/Entity）
│   │   └── tools/         # Agent 工具集
│   └── src/main/resources/
│       ├── application.yml
│       └── prompts/       # Prompt 模板
├── docker-compose.yml     # 本地开发环境
└── docs/                  # 项目文档
```

## API 文档

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/chat` | POST | 同步对话 |
| `/api/chat/stream` | POST | 流式对话（SSE） |
| `/api/knowledge/{id}/upload` | POST | 上传文档到知识库 |
| `/api/knowledge/{id}` | DELETE | 删除知识库 |
