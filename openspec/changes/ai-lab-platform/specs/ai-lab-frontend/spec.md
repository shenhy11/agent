## ADDED Requirements

### Requirement: AI Lab 入口页面
系统 SHALL 提供 `ai-lab.html` 页面，以卡片网格展示 6 大 AI Demo 模块，每个卡片包含图标、标题、简述和"体验"入口按钮。

#### Scenario: 访问 AI Lab 入口
- **WHEN** 用户点击导航栏的 "🧪 AI Lab"
- **THEN** 展示 6 个 Demo 卡片（智能客服、多模态、NLP 工具箱、RAG 知识库、Agent 工作流、能力评测），每个可点击进入

### Requirement: 智能客服 Demo 页面
系统 SHALL 提供 `demo-chat.html` 页面，包含对话面板和 Agent 思考链可视化面板，用户可实时体验 AI 客服对话并观察 Agent 推理过程。

#### Scenario: 对话并观察思考链
- **WHEN** 用户在 Demo 页发送 "推荐一款适合跑步的手表"
- **THEN** 左侧显示流式回答，右侧实时展示 Agent 的思考步骤、工具调用和耗时

### Requirement: 多模态 Demo 页面
系统 SHALL 提供 `demo-vision.html` 页面，包含图片上传区域和对话输入框，用户可上传图片并提问。

#### Scenario: 拖拽上传图片并提问
- **WHEN** 用户拖拽一张手表图片到上传区域并输入 "这是什么型号？"
- **THEN** 前端将图片和文字发送到后端，流式展示 AI 回答

### Requirement: NLP 工具箱 Demo 页面
系统 SHALL 提供 `demo-nlp.html` 页面，包含多 Tab 切换的文本分析工具界面（摘要/情感/关键词/翻译），左输入右输出布局。

#### Scenario: 切换到情感分析 Tab
- **WHEN** 用户点击"情感分析" Tab 并粘贴一段产品评论后点击"分析"
- **THEN** 右侧展示情感标签（正面/负面/中性）、置信度、提取的关键词

### Requirement: RAG 知识库 Demo 页面
系统 SHALL 提供 `demo-rag.html` 页面，包含三栏布局：知识库管理（左）、对话窗口（中）、检索来源可视化（右）。

#### Scenario: RAG 对话展示来源
- **WHEN** 用户问 "手表保修期是多久？"
- **THEN** 中间栏显示 AI 回答，右栏展示检索到的文档块来源（文件名、相似度、内容摘要）

### Requirement: Agent 工作流 Demo 页面
系统 SHALL 提供 `demo-agent.html` 页面，用户输入复杂任务后，页面以流程图形式可视化展示 Agent 的多步执行过程。

#### Scenario: 可视化多步任务
- **WHEN** 用户输入 "对比运动系列和商务系列，生成推荐报告"
- **THEN** 页面动态渲染 Agent 的执行步骤（查询 → 对比 → 分析 → 报告），每步显示状态和耗时

### Requirement: 导航栏新增 AI Lab 入口
系统 SHALL 在全站导航栏中新增 "🧪 AI Lab" 链接，位于"联系我们"之后。

#### Scenario: 所有页面可进入 AI Lab
- **WHEN** 用户在任意页面点击导航栏的 "AI Lab"
- **THEN** 导航到 `ai-lab.html` 页面

### Requirement: Demo 页面视觉一致性
所有 Demo 页面 SHALL 继承全站的深色科技风设计系统（CSS 变量、毛玻璃效果、渐变色），与现有页面风格统一。

#### Scenario: Demo 页面风格一致
- **WHEN** 用户从产品页跳转到 AI Lab
- **THEN** 配色、字体、导航栏、页脚风格完全一致
