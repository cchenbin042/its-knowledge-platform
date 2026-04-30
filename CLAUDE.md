# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 构建命令

```bash
# 构建所有模块
mvn clean install

# 构建指定模块（-am 同时构建依赖模块）
mvn clean install -pl platform-core -am

# 运行应用（从bootstrap模块启动）
mvn spring-boot:run -pl platform-bootstrap

# 指定profile运行
mvn spring-boot:run -pl platform-bootstrap -Dspring-boot.run.profiles=dev

# 打包部署
mvn clean package -pl platform-bootstrap -am
```

## 基础设施

```bash
# 启动所有基础设施（PostgreSQL、Redis、Elasticsearch、Milvus）
docker-compose up -d

# 查看服务状态
docker-compose ps

# 停止所有服务
docker-compose down
```

依赖服务：
- PostgreSQL 16（端口 5432）
- Redis 7（端口 6379）
- Elasticsearch 8.12（端口 9200）
- Milvus 2.4（端口 19530）

## 环境变量

LLM配置通过环境变量设置（默认值在 application-dev.yml）：
- `LLM_BASE_URL` - OpenAI兼容的API地址
- `LLM_API_KEY` - LLM服务API密钥
- `LLM_EMBEDDING_MODEL` - Embedding模型名称
- `LLM_CHAT_MODEL` - Chat模型名称

## 模块架构

7个Maven模块，严格依赖层级（从底到顶）：

```
platform-common     → 工具类、统一返回结构、异常定义、常量
platform-infra      → 基础设施：Milvus/ES/PostgreSQL/Redis客户端
platform-rag        → RAG核心算法：分块、检索、融合
platform-crawler    → iKnow爬虫（独立模块）
platform-core       → 业务编排：QueryService、IngestionService
platform-api        → REST控制器、DTO、全局异常处理
platform-bootstrap  → 启动模块、配置文件
```

**依赖规则**：底层模块不能依赖上层模块。`infra` 不能引用 `core` 的代码。

## 核心数据流

### 查询链路

```
QueryController → QueryService → CompositeRetriever
    ↓
    并行检索（JDK 21虚拟线程）：
    ├─ MilvusVectorStore（向量召回）
    └─ EsBm25Retriever（BM25全文检索）
    ↓
    合并结果 → 构建上下文 → ChatLanguageModel.generate()
```

核心类：`CompositeRetriever` 使用 `StructuredTaskScope` 实现并行检索。

### 入库链路

```
DocumentController → IngestionService
    ↓
    MarkdownChunker（按标题分块）
    ↓
    EmbeddingModel.embedAll() → MilvusVectorStore.addAll()
    ↓
    EsDocumentIndexer.indexDocument()
    ↓
    DocumentRepository.save()（PostgreSQL元数据）
```

通过MD5内容哈希 + PostgreSQL唯一索引实现去重。

## 配置结构

RAG参数在 `application.yml` 的 `rag.*` 下：

```yaml
rag:
  chunk:
    size: 2500      # 分块大小
    overlap: 300    # 重叠字符数
  retrieve:
    top-rough: 100  # 初步召回数量
    top-final: 8    # 最终返回数量
  rrf:
    k: 60           # RRF融合参数
```

## API接口

- `POST /query` - 同步查询，返回答案和来源
- `POST /documents/upload` - 上传Markdown文档入库
- `GET /documents` - 文档列表
- `GET /documents/{title}` - 文档详情
- `DELETE /documents/{title}` - 删除文档

## 数据库表

核心表结构（见 `init-scripts/init.sql`）：
- `documents` - 文档元数据，`content_hash`唯一索引去重
- `sessions` - 多轮会话，messages用JSONB存储
- `feedbacks` - 用户反馈
- `query_logs` - 查询日志统计

## 技术要点

- 使用JDK 21虚拟线程（`StructuredTaskScope`）做并行检索，无需线程池调优
- LangChain4j提供RAG抽象（EmbeddingModel、ChatLanguageModel、TextSegment）
- MyBatis-Plus作为ORM框架
- SSE流式输出计划用 `SseEmitter` 实现（尚未完成）
- 文档解析目前仅支持Markdown，计划用Tika支持多格式