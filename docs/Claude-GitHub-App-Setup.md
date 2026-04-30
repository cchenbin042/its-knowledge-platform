# Claude GitHub App 安装与项目推送指南

> 创建日期: 2026-04-30
> 适用项目: its-knowledge-platform

---

## 一、GitHub 创建仓库

### 步骤

1. 登录 GitHub → 点击右上角 `+` → `New repository`
2. 填写仓库信息：
   - Repository name: `its-knowledge-platform`
   - 选择 Public 或 Private
   - **不要勾选** "Add a README file"（避免本地冲突）
   - **不要勾选** "Add .gitignore" 和 "Choose a license"
3. 点击 `Create repository`

### 注意事项

- 创建后 GitHub 会显示一个空白仓库页面
- 页面会提示推送命令，保留此页面后续使用

---

## 二、安装 Claude GitHub App

### 步骤

1. **访问安装页面**

   直接链接：
   - https://github.com/apps/claude-code
   - 或 https://github.com/marketplace/claude-code

2. **点击 Install 按钮**

   页面右上角显示 `Install` 或 `Install it for free`

3. **选择仓库范围**

   | 选项 | 说明 | 推荐 |
   |---|---|---|
   | All repositories | 所有仓库可用 Claude | - |
   | Only select repositories | 只选特定仓库 | **推荐** |

   推荐选择 **Only select repositories** → 下拉选择 `its-knowledge-platform`

4. **确认安装**

   点击底部 `Install` 或 `Save` 按钮

5. **授权确认**

   如弹出授权页面，点击 `Authorize` 允许 Claude Code 访问仓库

### 安装后验证

安装成功后，在仓库设置中可查看：

`仓库页面 → Settings → Integrations → Claude Code`

---

## 三、推送代码到 GitHub

### 配置远程仓库

```bash
cd "D:/Java/项目/workSpace/its-knowledge-platform"

# 添加远程仓库（替换为你的实际地址）
git remote add origin https://github.com/<你的用户名>/its-knowledge-platform.git

# 查看配置
git remote -v
```

### 推送代码

```bash
# 推送所有本地提交到远程
git push -u origin master
```

### 验证推送成功

- 打开 GitHub 仓库页面
- 确认能看到所有文件和 commit 历史

---

## 四、添加 .gitignore

避免推送不必要的文件到仓库。

### 创建文件

```bash
cat > .gitignore << 'EOF'
# IDE
.idea/
*.iml
.vscode/

# Build
target/
*.jar
*.class

# Logs
*.log

# OS
.DS_Store
Thumbs.db

# Claude Code 本地配置（可选保留）
.claude/
EOF
```

### 提交推送

```bash
git add .gitignore
git commit -m "chore: 添加 .gitignore"
git push
```

---

## 五、创建 CLAUDE.md

让 Claude Code 了解项目背景和协作规范。

### 示例内容

```markdown
# ITS Knowledge Platform

## 项目简介
企业级IT支持RAG知识问答平台，从 Python 迁移到 Spring Boot 模块化单体架构。

## 技术栈
- Spring Boot 3.2.5 + JDK 21
- LangChain4j 0.35+
- Milvus 2.4（向量库）
- Elasticsearch 8 + IK分词（全文检索）
- PostgreSQL 16（元数据）
- Redis 7（缓存/会话）
- MyBatis-Plus
- Vue 3 + TypeScript（前端）

## Maven 模块结构
```
bootstrap → api → core → rag → infra → common
                    └─→ crawler ─→ infra
```

## 开发规范
- 回复使用中文，代码保持英文
- 每个功能模块独立 commit
- 遵循模块分层依赖，底层不依赖上层

## 实施计划
详细实施计划位于：docs/superpowers/plans/2026-04-29-springboot-rag-refactor.md

## 当前进度
- ✅ Phase 1: 项目骨架搭建（已完成）
- ⏳ Phase 2: 核心 RAG 功能（待执行）
- ⏳ Phase 3: 完整功能
- ⏳ Phase 4: 前端与部署

## API 端点
| 端点 | 功能 |
|---|---|
| POST /documents/upload | 上传文档入库 |
| GET /documents | 文档列表 |
| POST /query | RAG问答 |
```

### 提交推送

```bash
git add CLAUDE.md
git commit -m "docs: 添加 CLAUDE.md 项目协作指南"
git push
```

---

## 六、GitHub 中使用 Claude

安装 Claude GitHub App 后，可在 GitHub 网页端直接使用。

### 使用方式

| 场景 | 命令示例 |
|---|---|
| **Issue 中提问** | `@claude 这个bug如何修复？` |
| **PR 自动审查** | Claude 自动审查并评论 |
| **创建 PR** | `@claude create a PR for feature-x` |
| **执行任务** | `@claude implement the feature in issue #5` |
| **代码解释** | `@claude explain this code` |
| **生成文档** | `@claude generate README for this module` |

### Issue 自动化示例

创建 Issue 时可以这样描述：

```
@claude please implement the RRF fusion algorithm described in:
docs/superpowers/plans/2026-04-29-springboot-rag-refactor.md Task 11
```

Claude 会读取计划文档并自动实现。

---

## 七、常用命令汇总

| 命令 | 说明 |
|---|---|
| `git remote -v` | 查看远程仓库配置 |
| `git status` | 查看当前状态 |
| `git log --oneline -10` | 查看最近10条提交 |
| `git push` | 推送到远程 |
| `git pull` | 拉取远程更新 |

---

## 附录：问题排查

### 推送失败：remote rejected

原因：远程仓库已有文件（如 README）

解决：
```bash
git pull origin master --allow-unrelated-histories
git push -u origin master
```

### 推送失败：Authentication required

原因：未登录 GitHub

解决：
```bash
# 使用 GitHub CLI 登录
gh auth login

# 或使用 Personal Access Token
git remote set-url origin https://<token>@github.com/<用户名>/<仓库>.git
```

### Claude App 无响应

原因：未正确安装或仓库未授权

解决：
1. 访问 https://github.com/settings/installations
2. 检查 Claude Code 是否已安装
3. 确认仓库在授权列表中

---

## 参考链接

- Claude Code 官网: https://claude.ai/code
- GitHub App 安装: https://github.com/apps/claude-code
- Claude Code 文档: https://docs.anthropic.com/claude-code