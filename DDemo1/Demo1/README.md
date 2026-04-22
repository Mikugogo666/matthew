# AI 知识库工单助手 / Personal Site

这是一个基于 `Spring Boot` 和 `Spring AI` 构建的 `AI 知识库工单助手`。项目围绕知识导入、智能聊天和工单分析三个核心能力展开，技术栈包括：

- `Java 21`
- `Spring Boot`
- `Spring AI`
- `OpenAI 兼容接口`
- 简易 `RAG`
- 多轮上下文
- 本地工具命令

## 功能

### 1. 智能问答

通过 `Spring AI ChatClient` 接入模型，支持普通问答。

### 2. 多轮上下文

通过 `ChatMemory` 维护对话上下文。

### 3. 本地工具命令

支持：

- `/time`
- `/weather 北京`

### 4. 简易 RAG

支持将文本知识导入内存向量库，提问时先召回相关片段，再交给模型生成回答。

### 5. AI 工单助手

支持创建工单并自动返回：

- 工单分类
- 优先级
- 问题摘要
- 回复草稿
- 知识库引用

### 6. Mock Mode

如果没有配置真实模型密钥，接口仍可返回 Mock 响应，方便联调和功能验证。

## 目录结构

```text
Demo1
├─ pom.xml
├─ README.md
├─ run-demo1.bat
├─ run-demo1.ps1
└─ src
   └─ main
      ├─ java
      └─ resources
```

## 配置模型

运行前设置环境变量：

```powershell
$env:OPENAI_API_KEY="你的API密钥"
$env:OPENAI_BASE_URL="https://api.openai.com"
$env:OPENAI_MODEL="gpt-4.1-mini"
$env:OPENAI_EMBEDDING_MODEL="text-embedding-3-small"
```

如果没有配置这些变量，项目会进入 `Mock Mode`。

## GitHub Pages 个人博客

仓库内新增了 `docs/` 目录，可直接作为 GitHub Pages 的静态个人主页使用：

- 主页文件：`docs/index.html`
- 适合展示个人简介、项目经历、技术方向和博客卡片
- GitHub Pages 发布时选择 `Deploy from a branch`
- Branch 选 `main`
- Folder 选 `/docs`

这样做的原因是：GitHub Pages 只能直接托管静态页面，不能运行当前这个 Spring Boot 后端。因此仓库保留后端源码，同时用 `docs/` 提供个人博客页面。

## 启动

### 批处理

双击 `run-demo1.bat`

### PowerShell

```powershell
cd C:\Users\lenovo\Desktop\Demo1
powershell -ExecutionPolicy Bypass -File .\run-demo1.ps1
```

## 建议的仓库结构

```text
Demo1
├─ docs                 # GitHub Pages 个人主页
├─ src                  # Spring Boot + Spring AI 源码
├─ pom.xml
└─ README.md
```

## 接口

### 1. 聊天

`POST /api/chat`

```json
{
  "conversationId": "session-1",
  "message": "请帮我生成一段 AI Agent 岗位自我介绍",
  "useKnowledge": true
}
```

### 2. 导入知识文本

`POST /api/knowledge/text`

```json
{
  "title": "校园网处理规范",
  "content": "Spring AI 提供 ChatClient、ChatMemory、VectorStore 等能力。"
}
```

### 3. 查看知识库状态

`GET /api/knowledge`

### 4. 创建工单

`POST /api/tickets`

```json
{
  "customerName": "张三",
  "question": "校园网一直连不上，登录后提示认证失败，怎么办？",
  "useKnowledge": true
}
```

### 5. 查看工单列表

`GET /api/tickets`

### 6. 健康检查

`GET /actuator/health`
