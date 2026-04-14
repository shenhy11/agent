# 📊 项目总进度总览 — ChronoTech AI 能力展示平台

> **最后更新时间**: 2026-04-07 21:00
> **当前里程碑**: 阶段 5 · AI Lab 平台集成
> **总体进度**: ████████░░ 82%

---

## 一、开发流程总览

| 阶段 | 名称 | 状态 | 进度 | 预计周期 |
|------|------|------|------|----------|
| **1** | 需求确认 | ✅ 完成 | 100% | 0.5 天 |
| **2** | UI 设计 | ✅ 完成 | 100% | 1 天 |
| **3** | 接口设计 | ✅ 完成 | 100% | 0.5 天 |
| **4** | 前端开发 (官网) | ✅ 完成 | 100% | 2 天 |
| **5** | 后端开发 + AI Lab | 🟡 进行中 | 85% | 2-3 天 |
| **6** | 联调测试 | 🟡 部分完成 | 40% | 1 天 |

---

## 二、AI Lab 平台（核心新增）

### 后端 AI 能力
| 模块 | 状态 | 关键文件 | 说明 |
|------|------|---------|------|
| 产品 API | ✅ | `ProductController` + `ProductData` | 6 款产品硬编码，列表/详情/对比 |
| 联系表单 API | ✅ | `ContactController` + `ContactRequest` | 含参数校验 |
| RAG 知识库 | ✅ | `KnowledgeInitializer` + 3 份预置文档 | 启动自动加载 + QuestionAnswerAdvisor |
| Agent 工具集 | ✅ | `ProductTools` (4个@Tool) | 查询/对比/推荐/优惠 |
| NLP 文本分析 | ✅ | `TextAnalysisService` + `NlpController` | 摘要/情感/关键词/翻译 |
| 多模态视觉 | ✅ | `VisionService` + `MultimodalController` | 图文联合问答 + 格式校验 |
| SSE 流式对话 | ✅ | `ChatController` (GET + POST) | 兼容前端 Demo |
| System Prompt | ✅ | `system-prompt.st` | 适配 ChronoTech 手表场景 |

### 前端 AI Lab Demo 页
| 页面 | 状态 | 关键文件 | 说明 |
|------|------|---------|------|
| AI Lab 入口 | ✅ | `ai-lab.html` + `css/ai-lab.css` | 6 卡片 + 技术栈展示 |
| 智能客服 Demo | ✅ | `demo-chat.html` + `js/demo-chat.js` | 双栏(对话+思考链) + 预置问题 |
| 多模态 Demo | ✅ | `demo-vision.html` + `js/demo-vision.js` | 拖拽上传 + 图文问答 |
| NLP 工具箱 Demo | ✅ | `demo-nlp.html` + `js/demo-nlp.js` | 4 Tab + 示例文本 + 可视化 |
| RAG 知识库 Demo | ✅ | `demo-rag.html` + `js/demo-rag.js` | 三栏(知识库/对话/检索来源) |
| Agent 工作流 Demo | ✅ | `demo-agent.html` + `js/demo-agent.js` | 流程图可视化 + 预置任务 |
| 能力评测 | ⚪ | — | 占位卡片，标记"即将上线" |

### 官网前端（已完成）
| 任务项 | 状态 | 说明 |
|--------|------|------|
| 设计系统 (CSS 变量/token) | ✅ | `css/variables.css` |
| 基础样式 + 导航栏 + 页脚 | ✅ | `css/base.css` + `css/layout.css` |
| 首页 | ✅ | `index.html` |
| 产品列表页 | ✅ | `products.html`（含筛选 + 对比表） |
| 关于我们页 | ✅ | `about.html`（品牌故事 + 时间线） |
| 联系我们页 | ✅ | `contact.html`（表单 + FAQ） |
| 智能客服浮窗 | ✅ | SSE 流式对话组件 |
| 响应式适配 | ✅ | PC / 平板 / 手机三断点 |
| 全站 AI Lab 导航 | ✅ | 4 个页面均已添加 🧪 AI Lab 链接 |

---

## 三、技术栈

| 层级 | 技术 |
|------|------|
| **前端** | 原生 HTML/CSS/JS, Inter 字体, Font Awesome 图标 |
| **后端** | Java 21, Spring Boot 3.4, Spring AI 1.0 |
| **AI 底座** | 通义千问 (DashScope): qwen-plus / qwen-vl-max |
| **RAG** | SimpleVectorStore + Tika 文档解析 + TokenTextSplitter |
| **工具调用** | Spring AI @Tool 注解，ProductTools + CustomerServiceTools |
| **通信** | SSE (Server-Sent Events) 流式输出 |

---

## 四、文件结构

```
agent-web/                           # 前端
├── index.html                       # 首页
├── products.html                    # 产品列表
├── about.html                       # 关于我们
├── contact.html                     # 联系我们
├── ai-lab.html                      # AI Lab 入口
├── demo-chat.html                   # 智能客服 Demo
├── demo-vision.html                 # 多模态 Demo
├── demo-nlp.html                    # NLP 工具箱 Demo
├── demo-rag.html                    # RAG 知识库 Demo
├── demo-agent.html                  # Agent 工作流 Demo
├── css/
│   ├── variables.css / base.css     # 设计系统
│   ├── layout.css / pages.css       # 结构与页面
│   ├── ai-lab.css                   # AI Lab 专属样式
│   ├── responsive.css               # 响应式
│   └── chat-widget.css              # 客服浮窗
└── js/
    ├── main.js / chat-widget.js     # 全局 + 客服
    ├── demo-chat.js                 # 智能客服交互
    ├── demo-vision.js               # 多模态交互
    ├── demo-nlp.js                  # NLP 交互
    ├── demo-rag.js                  # RAG 交互
    └── demo-agent.js                # Agent 交互

agent-server/src/main/java/com/agent/ # 后端
├── config/
│   ├── AiConfig.java                # ChatClient + RAG + VectorStore
│   └── KnowledgeInitializer.java    # 知识库启动加载
├── controller/
│   ├── ChatController.java          # 对话 API (GET+POST SSE)
│   ├── ProductController.java       # 产品 API
│   ├── ContactController.java       # 联系表单 API
│   ├── NlpController.java           # NLP API
│   └── MultimodalController.java    # 多模态 API
├── service/
│   ├── chat/ChatService.java        # 对话服务
│   ├── nlp/TextAnalysisService.java # NLP 服务
│   └── multimodal/VisionService.java# 视觉服务
├── tools/
│   ├── ProductTools.java            # 产品工具(4个@Tool)
│   └── CustomerServiceTools.java    # 客服工具
└── model/
    ├── ProductData.java             # 产品硬编码数据
    └── dto/                         # DTO 类

docs/knowledge/                      # RAG 预置知识库
├── product-specs.md                 # 产品规格手册
├── faq.md                           # 常见问题
└── after-sales.md                   # 售后服务政策
```

---

## 五、待办事项（剩余）

| 任务 | 优先级 | 说明 |
|------|:---:|------|
| 端到端验证（RAG/Agent/NLP/多模态） | P0 | 需启动后端连接 DashScope |
| 响应式适配 Demo 页面 | P1 | 继承 responsive.css 断点 |
| 多模态预置示例图片 | P2 | 手表正面/屏幕异常 |
| 能力评测 Dashboard | P2 | metrics 后续迭代 |

---

## 六、关键文档

| 文档 | 路径 | 说明 |
|------|------|------|
| 需求文档 (PRD) | `docs/PRD-Website.md` | ✅ 已完成 |
| API 接口设计 | `docs/API-Design.md` | ✅ 已完成 |
| 架构设计 | `docs/Architecture-Design.md` | ✅ 后端 AI 架构 |
| AI Lab Proposal | `openspec/changes/ai-lab-platform/proposal.md` | ✅ 变更目标 |
| AI Lab Design | `openspec/changes/ai-lab-platform/design.md` | ✅ 技术决策 |
| AI Lab Tasks | `openspec/changes/ai-lab-platform/tasks.md` | 🟡 40/49 完成 |
