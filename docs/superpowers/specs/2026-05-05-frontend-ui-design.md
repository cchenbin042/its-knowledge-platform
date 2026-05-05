---
name: Frontend UI Design
description: ITS Knowledge Platform前端UI技术实现方案
type: project
created: 2026-05-05
---

# ITS Knowledge Platform 前端UI设计方案

## 设计决策

| 决策项 | 选择 |
|--------|------|
| 整体风格 | 浅色简洁风格（Slate灰白系） |
| 功能范围 | 完整功能集：查询对话、文档管理、统计看板、系统设置 |
| 查询交互 | 多轮对话模式，支持上下文追问 |
| 检索可视化 | 展示步骤进度（查询理解→知识检索→精排筛选→生成回答） |
| 深度模式 | 支持快速/深度模式切换，深度模式使用Agent多轮推理 |
| 统计页面 | 详细统计 + 模块化卡片设计 |
| 文档展示 | 卡片网格布局 |
| 技术方案 | 方案A：渐进式演进（在现有React骨架基础上完善） |

---

## 一、整体架构

### 页面结构

```
Layout（侧边栏布局）
├─ /query          → QueryPage（对话查询）
├─ /documents      → DocumentsPage（文档管理）
├─ /stats          → StatsPage（统计看板）
└─ /settings       → SettingsPage（系统设置）
```

### 组件层级

遵循现有原子设计模式：

```
atoms/          → 基础组件（已有：Button、Input、Card、Badge）
                → 新增：Spinner、Toggle、Select、ChartCard

molecules/      → 组合组件（已有：SourceCard、DocumentItem、SearchBox）
                → 新增：MessageItem、RetrievalSteps、StepItem、FeedbackBar、StatCard

organisms/      → 复杂组件（已有：QueryPanel、DocumentList）
                → 新增：ChatContainer、StatsDashboard、SettingsForm

pages/          → 页面组装
```

### 状态管理（Zustand）

```typescript
// 现有：useQueryStore（单次查询）
// 新增：
useChatStore     → 多轮对话消息列表、会话ID、历史记录
useSettingsStore → 用户偏好（主题、检索参数）
```

---

## 二、各页面设计详情

### 2.1 查询对话页面 (QueryPage)

**核心组件：ChatContainer**

```
结构：
┌─────────────────────────────────────────────────┐
│ Header: 会话标题 + 新对话按钮 + 模式切换开关      │
├─────────────────────────────────────────────────┤
│                                                 │
│  Messages Area (可滚动)                          │
│  ├─ MessageItem (用户消息)                       │
│  ├─ MessageItem (助手消息)                       │
│  │   ├─ RetrievalSteps (检索步骤可视化)          │
│  │   ├─ 回答内容 (Markdown渲染)                  │
│  │   ├─ Sources (参考来源标签)                   │
│  │   └─ FeedbackBar (点赞/点踩)                  │
│  ...                                            │
│                                                 │
├─────────────────────────────────────────────────┤
│ Input Area: 文本框 + 快速/深度切换 + 发送按钮     │
└─────────────────────────────────────────────────┘
```

**检索步骤定义：**

| 步骤Key | 显示名称 | 状态 |
|---------|---------|------|
| query_understanding | 查询理解 | pending/running/completed/error |
| knowledge_retrieval | 知识检索 | 同上 |
| reranking | 精排筛选 | 同上 |
| answer_generation | 生成回答 | 同上 |

**深度模式差异：**
- 快速模式：标准RAG流程，4步骤
- 深度模式：Agent多轮推理，动态展示agentRounds（包含tool_calls）

**SSE事件对接：**
- 后端 `StreamingQueryController` 已实现SSE
- 前端通过 `api.streamQuery()` 接收事件
- 事件类型：`retrieval_start`, `retrieval_vector`, `retrieval_bm25`, `retrieval_fused`, `context_built`, `llm_token`, `llm_done`, `error`, `complete`

---

### 2.2 文档管理页面 (DocumentsPage)

**展示形式：卡片网格**

```
结构：
┌─────────────────────────────────────────────────┐
│ Header: 文档管理 + 上传区域                      │
├─────────────────────────────────────────────────┤
│                                                 │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐         │
│  │ DocCard │  │ DocCard │  │ DocCard │         │
│  │ ├─图标  │  │ ├─图标  │  │ ├─图标  │         │
│  │ ├─标题  │  │ ├─标题  │  │ ├─标题  │         │
│  │ ├─切片数│  │ ├─切片数│  │ ├─切片数│         │
│  │ ├─时间  │  │ ├─时间  │  │ ├─时间  │         │
│  │ └─操作  │  │ └─操作  │  │ └─操作  │         │
│  └─────────┘  └─────────┘  └─────────┘         │
│                                                 │
└─────────────────────────────────────────────────┘
```

**卡片操作：**
- 点击卡片展开详情（全文预览）
- 删除按钮（带确认弹窗）
- 上传支持拖拽上传（Markdown文件）

---

### 2.3 统计看板页面 (StatsPage)

**模块化卡片设计：**

```
结构：
┌─────────────────────────────────────────────────┐
│ Header: 统计看板                                │
├─────────────────────────────────────────────────┤
│                                                 │
│  ┌───────────────────┐  ┌───────────────────┐  │
│  │ OverviewCard      │  │ FeedbackCard      │  │
│  │ ├─文档总数        │  │ ├─好评率          │  │
│  │ ├─查询次数        │  │ ├─评分分布图      │  │
│  │ ├─平均响应时间    │  │ └─最近反馈列表    │  │
│  └───────────────────┘  └───────────────────┘  │
│                                                 │
│  ┌───────────────────┐  ┌───────────────────┐  │
│  │ RetrievalCard     │  │ TimelineCard      │  │
│  │ ├─向量命中率      │  │ ├─时段查询趋势图  │  │
│  │ ├─BM25命中率      │  │ └─高峰时段标注    │  │
│  │ └─Top问题排行     │  │ └───────────────┘  │
│  └───────────────────┘                          │
│                                                 │
└─────────────────────────────────────────────────┘
```

**数据指标：**

| 类别 | 指标 |
|------|------|
| Overview | 文档总数、查询次数、平均响应时间 |
| Feedback | 好评率、评分分布 |
| Retrieval | 向量命中率、BM25命中率、Top10热门问题 |
| Timeline | 24小时查询趋势、周查询趋势 |

**图表库选择：**
- 推荐使用 `recharts`（React生态，轻量级）
- 或 `chart.js` + `react-chartjs-2`

---

### 2.4 系统设置页面 (SettingsPage)

**设置项：**

```
结构：
┌─────────────────────────────────────────────────┐
│ Header: 系统设置                                │
├─────────────────────────────────────────────────┤
│                                                 │
│  ┌─────────────────────────────────────────┐   │
│  │ 检索参数设置                             │   │
│  │ ├─ Top-K (召回数量)        [滑块: 5-20]  │   │
│  │ ├─ RRF K (融合参数)        [滑块: 40-80] │   │
│  │ └─ 最小相关度阈值          [滑块: 0-1]   │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  ┌─────────────────────────────────────────┐   │
│  │ 界面偏好                                 │   │
│  │ ├─ 默认查询模式           [快速/深度]    │   │
│  │ └─ 步骤展示默认折叠       [是/否]        │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
│  ┌─────────────────────────────────────────┐   │
│  │ LLM配置（展示，不可编辑）                │   │
│  │ ├─ API地址               [环境变量]      │   │
│  │ ├─ Embedding模型          [环境变量]     │   │
│  │ └─ Chat模型               [环境变量]     │   │
│  └─────────────────────────────────────────┘   │
│                                                 │
└─────────────────────────────────────────────────┘
```

---

## 三、技术栈确认

| 项 | 技术 | 说明 |
|----|------|------|
| 框架 | React 18 | 已有骨架 |
| 状态管理 | Zustand | 已有骨架，需扩展 |
| 样式 | TailwindCSS | 已配置 |
| 路由 | React Router v6 | 已配置 |
| HTTP | Axios | 已配置 |
| 图标 | Lucide React | 已配置 |
| 图表 | Recharts | 新增依赖 |
| Markdown渲染 | react-markdown | 新增依赖 |
| SSE处理 | Fetch + ReadableStream | 已有骨架 |

---

## 四、与后端API对接

| 前端调用 | 后端接口 | 说明 |
|---------|---------|------|
| `api.query()` | `POST /query` | 同步查询（备用） |
| `api.streamQuery()` | `POST /query/stream` | SSE流式查询（主用） |
| `api.getDocuments()` | `GET /documents` | 文档列表 |
| `api.uploadDocument()` | `POST /documents/upload` | 文档上传 |
| `api.deleteDocument()` | `DELETE /documents/{title}` | 文档删除 |
| 待实现 | `GET /stats` | 统计数据 |
| 待实现 | `POST /feedback` | 用户反馈 |

---

## 五、实现优先级

| Phase | 内容 | 依赖 |
|-------|------|------|
| Phase 1 | 查询对话页面完善 | 无 |
| Phase 2 | 文档管理页面完善 | 无 |
| Phase 3 | 统计看板页面 | 后端 `/stats` API |
| Phase 4 | 系统设置页面 | 后端 `/settings` API |

建议按Phase顺序实现，Phase 1-2可并行开发。