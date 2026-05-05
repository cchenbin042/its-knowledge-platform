---
name: Frontend Implementation Plan
description: ITS Knowledge Platform前端UI实施计划 — 4 Phase渐进式演进
type: project
created: 2026-05-05
depends_on: 2026-05-05-frontend-ui-design.md
---

# ITS Knowledge Platform 前端UI实施计划

## Context

基于 [前端UI设计方案](./2026-05-05-frontend-ui-design.md)，在当前 React 18 + TypeScript + Zustand + TailwindCSS 骨架基础上，按 Superpowers 渐进式演进策略（方案A），分4个 Phase 完成查询对话、文档管理、统计看板、系统设置四大页面。

---

## 一、现有骨架分析

### 已有组件（可复用）

| 组件 | 路径 | 状态 |
|------|------|------|
| Button, Input, Card, Badge | `atoms/` | 完整，支持 variant/size/loading |
| SearchBox, SourceCard, DocumentItem | `molecules/` | 完整 |
| QueryPanel, DocumentList | `organisms/` | 完整（需增强） |
| Layout (Sidebar) | `Layout.tsx` | 完整，响应式导航 |
| useDocumentsStore, useQueryStore | `stores/` | 完整 |
| ApiClient | `api/` | 完整（SSE支持已有） |

### 后端已实现API（前端未对接）

| 接口 | 状态 |
|------|------|
| `POST /query/stream` (SSE) | ✅ 后端已完成 |
| `POST/GET /sessions` | ✅ 后端已完成 |
| `GET /stats`, `/stats/daily`, `/stats/top-questions` | ✅ 后端已完成 |
| `POST /feedback`, `GET /feedback/stats` | ✅ 后端已完成 |

---

## 二、技术栈（无需新增依赖）

| 项 | 已安装 | 用途 |
|----|--------|------|
| React 18 + TypeScript | ✅ | 框架 |
| Zustand 4 | ✅ | 状态管理 |
| TailwindCSS 3 | ✅ | 样式 |
| React Router v6 | ✅ | 路由 |
| Axios 1.7 | ✅ | HTTP |
| Lucide React | ✅ | 图标 |
| clsx | ✅ | 条件class |

**不新增外部依赖** — 图表用纯CSS实现（进度条、柱状图），Markdown渲染用现有的 `whitespace-pre-wrap` 方式。

---

## 三、实施计划

### Phase 1: 查询对话页面增强

**工作量**: ~3h  
**目标**: 多轮对话模式 + 检索步骤可视化 + 深度模式切换

**文件变更**:

| 文件 | 操作 | 说明 |
|------|------|------|
| `src/components/atoms/Spinner.tsx` | **新增** | 加载动画组件 |
| `src/components/atoms/Toggle.tsx` | **新增** | 快速/深度模式切换开关 |
| `src/components/molecules/StepItem.tsx` | **新增** | 检索步骤单项（图标+名称+状态） |
| `src/components/molecules/RetrievalSteps.tsx` | **新增** | 4步骤进度条组件 |
| `src/components/molecules/FeedbackBar.tsx` | **新增** | 点赞/点踩按钮组 |
| `src/components/molecules/MessageItem.tsx` | **新增** | 聊天消息项（用户/助手/检索步骤） |
| `src/components/organisms/ChatContainer.tsx` | **新增** | 聊天容器（消息列表+输入区） |
| `src/stores/useChatStore.ts` | **新增** | 多轮对话状态管理 |
| `src/api/index.ts` | **修改** | 新增 sessions, feedback API |
| `src/pages/QueryPage.tsx` | **修改** | 重构为 ChatContainer 模式 |
| `src/main.tsx` | **修改** | 新增 /stats 路由 |
| `src/types/index.ts` | **修改** | 新增 Message, SSEEvent 类型 |

**关键交互逻辑**:

```
后端 SSE 事件 → useChatStore 处理:
  retrieval_start  → 初始化 RetrievalSteps
  retrieval_*      → 更新对应步骤状态
  llm_token        → 追加回答文本
  llm_done / error → 结束流
```

**ChatContainer 结构**:
```
┌─────────────────────────────────────────────────┐
│ Header: 会话标题 + 新对话按钮 + 模式切换开关      │
├─────────────────────────────────────────────────┤
│ Messages Area (可滚动)                           │
│ ├─ MessageItem (用户消息, 右对齐蓝色气泡)          │
│ ├─ MessageItem (助手消息, 左对齐)                 │
│ │   ├─ RetrievalSteps (4步骤进度条)              │
│ │   ├─ 回答内容                                   │
│ │   ├─ Sources (参考来源Badge标签)               │
│ │   └─ FeedbackBar (点赞/点踩)                    │
├─────────────────────────────────────────────────┤
│ Input Area: SearchBox + 快速/深度Toggle + 发送按钮  │
└─────────────────────────────────────────────────┘
```

**检索步骤定义**:

| 步骤Key | 显示名称 | 状态 |
|---------|---------|------|
| query_understanding | 查询理解 | pending/running/completed/error |
| knowledge_retrieval | 知识检索 | 同上 |
| reranking | 精排筛选 | 同上 |
| answer_generation | 生成回答 | 同上 |

**MessageItem 样式规范**:
- 用户消息: 右对齐, `bg-primary-500 text-white rounded-2xl`
- 助手消息: 左对齐, `bg-white border border-slate-200 rounded-2xl`
- 状态颜色: pending=`text-slate-400`, running=`text-blue-500 animate-pulse`, completed=`text-green-500`, error=`text-red-500`

**验证**: `npm run dev` → 打开 localhost:3000 → 输入问题 → 观察SSE流式回答 + 检索步骤动画

---

### Phase 2: 文档管理页面增强

**工作量**: ~2h  
**目标**: 卡片网格布局 + 拖拽上传 + 详情展开

**文件变更**:

| 文件 | 操作 | 说明 |
|------|------|------|
| `src/components/molecules/UploadZone.tsx` | **新增** | 拖拽上传区域组件 |
| `src/components/organisms/DocumentGrid.tsx` | **新增** | 卡片网格布局容器 |
| `src/pages/DocumentsPage.tsx` | **修改** | 集成 DocumentGrid + UploadZone |
| `src/components/molecules/DocumentItem.tsx` | **修改** | 增强为卡片样式（图标+标题+分块数+时间） |

**DocumentGrid 结构**:
```
┌─────────────────────────────────────────────────┐
│ Header: 文档管理 + 上传区域                      │
├─────────────────────────────────────────────────┤
│  ┌─────────┐  ┌─────────┐  ┌─────────┐         │
│  │ DocCard │  │ DocCard │  │ DocCard │         │
│  │ ├─图标  │  │ ├─图标  │  │ ├─图标  │         │
│  │ ├─标题  │  │ ├─标题  │  │ ├─标题  │         │
│  │ ├─切片数│  │ ├─切片数│  │ ├─切片数│         │
│  │ ├─时间  │  │ ├─时间  │  │ ├─时间  │         │
│  │ └─操作  │  │ └─操作  │  │ └─操作  │         │
│  └─────────┘  └─────────┘  └─────────┘         │
└─────────────────────────────────────────────────┘
```

**卡片操作**:
- 点击卡片展开详情（全文预览）
- 删除按钮（带确认弹窗）
- 上传支持拖拽上传（Markdown文件）

**验证**: 上传Markdown文件 → 卡片网格展示 → 删除文档 → 列表更新

---

### Phase 3: 统计看板页面

**工作量**: ~2h  
**目标**: 模块化卡片 + CSS图表 + 热门问题排行

**文件变更**:

| 文件 | 操作 | 说明 |
|------|------|------|
| `src/components/molecules/StatCard.tsx` | **新增** | 统计指标卡片 |
| `src/components/organisms/StatsDashboard.tsx` | **新增** | 统计看板容器 |
| `src/stores/useStatsStore.ts` | **新增** | 统计数据状态管理 |
| `src/api/index.ts` | **修改** | 新增 stats API |
| `src/pages/StatsPage.tsx` | **新增** | 统计页面 |
| `src/main.tsx` | **修改** | 新增 /stats 路由 |

**数据指标（纯CSS图表实现）**:

| 模块 | 数据来源 | 展示方式 |
|------|---------|---------|
| 总览卡片 | `GET /stats` | 数字 + 趋势箭头 |
| 日查询趋势 | `GET /stats/daily` | CSS柱状图 |
| 热门问题 | `GET /stats/top-questions` | 排名列表 + 次数 |
| 反馈统计 | `GET /feedback/stats` | 进度条（好评率） |

**StatsDashboard 结构**:
```
┌─────────────────────────────────────────────────┐
│ Header: 统计看板                                │
├─────────────────────────────────────────────────┤
│  ┌───────────────────┐  ┌───────────────────┐  │
│  │ OverviewCard      │  │ FeedbackCard      │  │
│  │ ├─文档总数        │  │ ├─好评率          │  │
│  │ ├─查询次数        │  │ ├─评分分布图      │  │
│  │ ├─平均响应时间    │  │ └─最近反馈列表    │  │
│  └───────────────────┘  └───────────────────┘  │
│  ┌───────────────────┐  ┌───────────────────┐  │
│  │ RetrievalCard     │  │ TimelineCard      │  │
│  │ ├─向量命中率      │  │ ├─时段查询趋势图  │  │
│  │ ├─全文检索命中率  │  │ └─高峰时段标注    │  │
│  │ └─Top问题排行     │  │                   │  │
│  └───────────────────┘  └───────────────────┘  │
└─────────────────────────────────────────────────┘
```

**验证**: 打开 /stats → 数据加载 → 卡片渲染 → 图表显示

---

### Phase 4: 系统设置页面

**工作量**: ~1h  
**目标**: 检索参数调整 + LLM配置展示

**文件变更**:

| 文件 | 操作 | 说明 |
|------|------|------|
| `src/components/atoms/Slider.tsx` | **新增** | 滑块组件 |
| `src/components/organisms/SettingsForm.tsx` | **新增** | 设置表单容器 |
| `src/stores/useSettingsStore.ts` | **新增** | 设置状态管理 |
| `src/api/index.ts` | **修改** | 新增 settings API（预留） |
| `src/pages/SettingsPage.tsx` | **新增** | 设置页面 |
| `src/main.tsx` | **修改** | 新增 /settings 路由 |

**设置项**:
- top-k 检索数量 (滑块 1-50, 默认8)
- RRF 融合参数 k (滑块 20-100, 默认60)
- 最小相关度阈值 (滑块 0-1, 步长0.1)
- 默认查询模式 (快速/深度)
- 步骤展示默认折叠 (是/否)
- LLM 配置展示 (API地址/Embedding模型/Chat模型, 只读)

**SettingsForm 结构**:
```
┌─────────────────────────────────────────────────┐
│ 检索参数设置                                    │
│ ├─ Top-K (召回数量)    [滑块: 1-50] 当前: 8    │
│ ├─ RRF K (融合参数)    [滑块: 20-100] 当前: 60 │
│ └─ 最小相关度阈值      [滑块: 0-1] 步长: 0.1   │
├─────────────────────────────────────────────────┤
│ 界面偏好                                        │
│ ├─ 默认查询模式        [快速/深度]              │
│ └─ 步骤展示默认折叠    [是/否]                  │
├─────────────────────────────────────────────────┤
│ LLM配置（展示）                                 │
│ ├─ API地址             [环境变量, 只读]          │
│ ├─ Embedding模型        [环境变量, 只读]         │
│ └─ Chat模型             [环境变量, 只读]         │
└─────────────────────────────────────────────────┘
```

**验证**: 打开 /settings → 调整参数 → 刷新页面 → 设置保持

---

## 四、开发顺序

```
Phase 1 (查询对话) ──→ Phase 2 (文档管理) ──→ Phase 3 (统计看板) ──→ Phase 4 (系统设置)
   ↓ 优先                    ↓ 其次                  ↓ 依赖后端API           ↓ 最后
```

Phase 1-2 可并行开发，Phase 3-4 可并行开发。

---

## 五、最终页面路由

```
Layout（侧边栏布局, 4个导航入口）
├─ /query          → QueryPage（对话查询）
├─ /documents      → DocumentsPage（文档管理）
├─ /stats          → StatsPage（统计看板）
└─ /settings       → SettingsPage（系统设置）
```

---

## 六、命名规范（遵循现有模式）

- 组件文件: `PascalCase.tsx`
- Store: `camelCase` hook (如 `useChatStore`)
- API: 统一在 `ApiClient` 类中扩展
- 类型: `src/types/index.ts` 统一管理
- 样式: Tailwind utility class + `clsx` 条件拼接
- 无新增外部依赖

---

## 七、验证清单

| Phase | 验证项 |
|-------|--------|
| 1 | SSE流式回答正常显示、检索步骤动画、快速/深度模式切换、历史会话列表 |
| 2 | 拖拽上传Markdown文件、卡片网格展示、删除确认、详情展开 |
| 3 | 统计卡片数据加载、日查询柱状图、热门问题排名、反馈数据 |
| 4 | 滑块调整参数、设置持久化、LLM配置只读展示 |

每个Phase完成后运行 `npm run build` 确保无编译错误。