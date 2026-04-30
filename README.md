# ITS Knowledge Platform

## 项目简介

ITS Knowledge Platform（企业级IT支持RAG知识问答平台）是一个基于 RAG（检索增强生成）技术的企业知识管理系统。该平台能够将企业内部的 Markdown 文档进行智能分块、向量化存储，并通过混合检索（向量检索 + BM25 全文检索）技术，结合大语言模型为用户提供精准的知识问答服务。

### 主要特性

- **智能文档入库**：支持 Markdown 文档上传，自动按标题层级分块，实现内容去重
- **混合检索策略**：并行执行向量检索（Milvus）和 BM25 全文检索（Elasticsearch），通过 RRF 算法融合结果
- **虚拟线程并发**：基于 JDK 21 虚拟线程（StructuredTaskScope）实现高效并行检索，无需线程池调优
- **多数据源支持**：PostgreSQL 元数据存储、Milvus 向量数据库、Elasticsearch 全文索引、Redis 缓存
- **OpenAI 兼容接口**：支持任何 OpenAI 兼容的 LLM 服务（如 OpenAI、Azure、本地部署模型等）

---

## 技术栈

| 类别 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 核心框架 | Spring Boot | 3.2.5 | 基础框架 |
| JDK 版本 | Java | 21 | 支持虚拟线程 |
| RAG 框架 | LangChain4j | 0.35.0 | LLM 抽象层 |
| ORM 框架 | MyBatis-Plus | 3.5.5 | 数据库访问 |
| 向量数据库 | Milvus | 2.4 | 向量存储与检索 |
| 全文检索 | Elasticsearch | 8.12 | BM25 全文检索 |
| 关系数据库 | PostgreSQL | 16 | 元数据持久化 |
| 缓存 | Redis | 7 | 查询结果缓存 |

---

## 系统架构

### 模块结构

项目采用 Maven 多模块架构，严格遵循依赖层级：

```
its-knowledge-platform
├── platform-common      # 公共模块：工具类、统一返回、异常定义、常量
├── platform-infra       # 基础设施：Milvus/ES/PostgreSQL/Redis 客户端
├── platform-rag         # RAG 核心：分块算法、检索融合、LLM 集成
├── platform-crawler     # 爬虫模块：iKnow 知识库爬取（独立）
├── platform-core        # 业务编排：QueryService、IngestionService
├── platform-api         # 接口层：REST 控制器、DTO、异常处理
└── platform-bootstrap   # 启动模块：配置文件、应用入口
```

**依赖规则**：底层模块不依赖上层模块。例如 `infra` 不能引用 `core` 的代码。

### 核心数据流

**查询链路**

```
QueryController → QueryService → CompositeRetriever
    ↓
    并行检索（虚拟线程）：
    ├─ MilvusVectorStore（向量召回）
    └─ EsBm25Retriever（BM25 全文检索）
    ↓
    RRF 融合 → 构建上下文 → ChatLanguageModel.generate()
    ↓
    返回答案 + 来源文档
```

**入库链路**

```
DocumentController → IngestionService
    ↓
    MarkdownChunker（按标题分块）
    ↓
    EmbeddingModel.embedAll() → MilvusVectorStore.addAll()
    ↓
    EsDocumentIndexer.indexDocument()
    ↓
    DocumentRepository.save()（PostgreSQL 元数据）
```

通过 MD5 内容哈希 + PostgreSQL 唯一索引实现文档去重。

---

## 环境要求

- JDK 21+
- Maven 3.8+
- Docker & Docker Compose（用于基础设施）
- 可访问的 LLM 服务（OpenAI 兼容接口）

---

## 安装步骤

### 1. 克隆项目

```bash
git clone https://github.com/your-org/its-knowledge-platform.git
cd its-knowledge-platform
```

### 2. 启动基础设施

```bash
# 启动 PostgreSQL、Redis、Elasticsearch、Milvus
docker-compose up -d

# 查看服务状态
docker-compose ps

# 等待所有服务健康（约 30-60 秒）
```

服务端口：
- PostgreSQL: `5432`
- Redis: `6379`
- Elasticsearch: `9200`
- Milvus: `19530`

### 3. 配置 LLM 服务

设置环境变量（或直接修改 `application-dev.yml`）：

```bash
# Windows PowerShell
$env:LLM_BASE_URL = "https://api.openai.com/v1"
$env:LLM_API_KEY = "your-api-key"
$env:LLM_EMBEDDING_MODEL = "text-embedding-3-small"
$env:LLM_CHAT_MODEL = "gpt-4o-mini"

# Linux/Mac
export LLM_BASE_URL="https://api.openai.com/v1"
export LLM_API_KEY="your-api-key"
export LLM_EMBEDDING_MODEL="text-embedding-3-small"
export LLM_CHAT_MODEL="gpt-4o-mini"
```

### 4. 构建项目

```bash
# 构建所有模块
mvn clean install

# 或构建指定模块（-am 同时构建依赖模块）
mvn clean install -pl platform-core -am
```

### 5. 启动应用

```bash
# 从 bootstrap 模块启动
mvn spring-boot:run -pl platform-bootstrap

# 指定 profile 运行
mvn spring-boot:run -pl platform-bootstrap -Dspring-boot.run.profiles=dev
```

应用启动后访问：`http://localhost:8080`

---

## 使用方法

### API 接口

#### 1. 知识问答

```bash
# POST /query - 知识问答
curl -X POST http://localhost:8080/query \
  -H "Content-Type: application/json" \
  -d '{"question": "如何配置数据库连接？"}'
```

响应示例：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "answer": "数据库连接配置位于 application.yml...",
    "sources": [
      {"title": "配置指南", "content": "...", "score": 0.85}
    ]
  }
}
```

#### 2. 文档管理

```bash
# 上传 Markdown 文档
curl -X POST http://localhost:8080/documents/upload \
  -F "file=@docs/guide.md"

# 获取文档列表
curl http://localhost:8080/documents

# 获取文档详情
curl http://localhost:8080/documents/配置指南

# 删除文档
curl -X DELETE http://localhost:8080/documents/配置指南
```

---

## 配置说明

### RAG 参数配置

在 `application.yml` 的 `rag.*` 下配置：

```yaml
rag:
  chunk:
    size: 2500        # 分块大小（字符数）
    overlap: 300      # 分块重叠（字符数）
  retrieve:
    top-rough: 100    # 初步召回数量
    top-final: 8      # 最终返回数量
    weights:
      vector: 0.6     # 向量检索权重
      bm25: 0.4       # BM25 检索权重
  rrf:
    k: 60             # RRF 融合参数
  cache:
    query-ttl-seconds: 300  # 查询缓存 TTL
```

### LLM 配置

```yaml
llm:
  base-url: ${LLM_BASE_URL:http://localhost:8000}
  api-key: ${LLM_API_KEY:sk-placeholder}
  embedding-model: ${LLM_EMBEDDING_MODEL:text-embedding-3-small}
  chat-model: ${LLM_CHAT_MODEL:gpt-4o-mini}
```

---

## 目录结构

```
its-knowledge-platform/
├── pom.xml                          # 父 POM
├── docker-compose.yml               # 基础设施编排
├── init-scripts/
│   └── init.sql                     # 数据库初始化脚本
├── platform-common/                 # 公共模块
│   └── src/main/java/com/its/platform/common/
│       ├── result/                  # 统一返回结构
│       ├── exception/               # 业务异常
│       └── constant/                # 常量定义
├── platform-infra/                  # 基础设施模块
│   └── src/main/java/com/its/platform/infra/
│       ├── milvus/                  # Milvus 向量存储
│       ├── es/                      # Elasticsearch 全文索引
│       └── postgres/                # PostgreSQL 数据访问
├── platform-rag/                    # RAG 核心模块
│   └── src/main/java/com/its/platform/rag/
│       ├── splitter/                # 文档分块器
│       ├── retriever/               # 混合检索器
│       └── llm/                     # LLM 配置
├── platform-crawler/                # 爬虫模块（独立）
├── platform-core/                   # 业务编排模块
│   └── src/main/java/com/its/platform/core/
│       ├── query/                   # 查询服务
│       └ ingestion/                 # 入库服务
├── platform-api/                    # API 模块
│   └── src/main/java/com/its/platform/api/
│       ├── controller/              # REST 控制器
│       └ dto/                       # 数据传输对象
├── platform-bootstrap/              # 启动模块
│   └ src/main/java/com/its/platform/
│   │   └ Application.java          # 应用入口
│   └ src/main/resources/
│       ├── application.yml          # 主配置
│       └ application-dev.yml        # 开发环境配置
└── CLAUDE.md                        # Claude Code 开发指南
```

---

## 数据库表结构

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| documents | 文档元数据 | id, title, content_hash(唯一), chunk_count |
| sessions | 多轮会话 | id, messages(JSONB), expires_at |
| feedbacks | 用户反馈 | session_id, question, answer, rating |
| query_logs | 查询日志 | session_id, duration_ms, source_count, cache_hit |

---

## 开发指南

```bash
# 打包部署
mvn clean package -pl platform-bootstrap -am

# 运行测试
mvn test

# 查看依赖树
mvn dependency:tree
```

---

## 许可证

MIT License

---

## 联系方式

如有问题或建议，请提交 Issue 或联系开发团队。