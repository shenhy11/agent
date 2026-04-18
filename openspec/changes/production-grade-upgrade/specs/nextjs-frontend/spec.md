## ADDED Requirements

### Requirement: Next.js 14 App Router 应用
系统 SHALL 使用 Next.js 14 (App Router) + React 18 构建前端应用，替代现有纯静态 HTML。

#### Scenario: 首页加载
- **WHEN** 用户访问根路径 `/`
- **THEN** Next.js SPA 加载，展示 Dashboard 首页，无全量页面刷新

### Requirement: 登录鉴权流程
前端 SHALL 实现登录页面和 Next.js Middleware 路由守卫，未登录用户自动跳转至 `/login`。

#### Scenario: 未登录访问受保护页面
- **WHEN** 未登录用户访问 `/ai-lab/chat`
- **THEN** Next.js Middleware 自动跳转至 `/login` 页面

#### Scenario: 登录成功跳转
- **WHEN** 用户输入正确的用户名密码并提交
- **THEN** 获取 JWT Token 存储至 Cookie（httpOnly 推荐）或 localStorage，跳转至原始目标页面

### Requirement: API 层统一封装
前端 SHALL 通过 axios 封装统一的 API 请求层，自动注入 JWT Token 和处理 401 响应。

#### Scenario: Token 自动注入
- **WHEN** 前端调用任何 API
- **THEN** axios 拦截器自动在 Header 中添加 `Authorization: Bearer <token>`

#### Scenario: Token 过期自动处理
- **WHEN** API 返回 401
- **THEN** 前端尝试用 refreshToken 刷新；若失败，跳转登录页

### Requirement: Shadcn/ui + Tailwind CSS 暗色主题
前端 SHALL 使用 Shadcn/ui 组件库 + Tailwind CSS，默认启用暗色主题。

#### Scenario: 暗色主题渲染
- **WHEN** 用户首次访问
- **THEN** 页面以暗色主题渲染，色调与当前 CSS 变量方案一致

### Requirement: 页面组件化迁移
前端 SHALL 将现有 11 个 HTML 页面的功能完整迁移至 React 组件。

#### Scenario: NLP Demo 功能完整性
- **WHEN** 用户访问 `/ai-lab/nlp`
- **THEN** 所有 8 个 NLP Tab（摘要/情感/关键词/翻译/NER/意图/长文档/Pipeline）功能正常

#### Scenario: Vision Demo 功能完整性
- **WHEN** 用户访问 `/ai-lab/vision`
- **THEN** 所有 5 个 Vision Tab（单图/多图/OCR/描述/搜图）功能正常

### Requirement: Zustand 状态管理
前端 SHALL 使用 Zustand 管理全局状态（认证状态、聊天会话、NLP/Vision 配置）。

#### Scenario: 认证状态持久化
- **WHEN** 用户刷新页面
- **THEN** Zustand 从 localStorage 恢复 Token，用户保持登录状态
