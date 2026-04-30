# Spring Boot 单体 RAG 知识平台 - 技术方案

> **文档版本**: v1.0
> **创建日期**: 2026-04-29
> **架构定位**: 模块化单体（Maven 多模块），未来可按需拆分微服务
> **适用场景**: 企业级 IT 支持 RAG 知识问答平台

---

## 一、背景与目标

### 1.1 重构动机
- **技术栈统一**：团队主要使用 Java，需要纳入统一技术体系便于维护和协作
- **性能与并发**：Python 在高并发、长连接场景存在瓶颈，期望用 JVM 提升吞吐
- **企业级特性**：需要 Spring 生态的事务、安全、监控、配置中心等能力

### 1.2 设计原则
- **保留核心功能**：完整迁移现有 RAG 流水线、Agent 模式、会话管理、统计分析
- **可发散拓展**：模块边界清晰，未来可按需拆分微服务或增加新能力
- **警惕过度设计**：拒绝一上来就 Spring Cloud 全家桶 / DDD 四层架构 / 微服务化

### 1.3 架构选型结论
经过充分讨论，最终采用 **Maven 多模块单体架构**，理由：
- 业务复杂度不够：核心仅 ingest 和 query 两条链路
- 没有微服务要解决的痛点：无多团队协作、无差异化扩缩容需求
- 微服务代价高：3-4 周额外工时全花在基础设施上，调试运维负担重
- 模块化单体保留了未来拆分微服务的可能性，前提是模块边界划清楚

---

## 二、技术栈

| 层级 | 选型 | 理由/备注 |
|---|---|---|
| 框架 | Spring Boot 3.2.x + JDK 21 | 虚拟线程跑并行检索，零调优负担 |
| RAG 框架 | LangChain4j 0.35+ | 比 Spring AI 成熟，社区活跃，文档/检索/Agent 抽象完整 |
| 向量库 | **Milvus 2.4** | LangChain4j 原生支持，中文社区活跃，性能强 |
| 全文检索 | Elasticsearch 8 + IK 分词器 | 替代原 Jaccard/TF-IDF 标题匹配，BM25 天然支持中文 |
| 关系库 | PostgreSQL 16 | 元数据、会话、反馈、统计 |
| 缓存 | Redis 7 | 替代 Python 的 cachetools/TTLCache，支持分布式 |
| ORM | MyBatis-Plus | 国内团队熟悉，比 JPA 灵活 |
| 流式 | Spring MVC + SseEmitter | 单体场景够用，比 WebFlux 简单 |
| 文档解析 | Apache Tika + commonmark-java | 替代 LangChain 的 loader 体系 |
| LLM 接入 | LangChain4j 的 OpenAI 兼容客户端 | 直连原 BASE_URL，零成本迁移 |
| 前端 | Vue 3 + TypeScript + Vite + Element Plus | 沿用技术栈，补 TS |
| 部署 | Docker Compose + Nginx | 沿用现有方案 |

---

## 三、Maven 多模块结构

### 3.1 目录组织

```
its-knowledge-platform/
├── pom.xml                                 # 父 POM，统一版本管理
├── platform-bootstrap/                     # 启动模块
│   ├── src/main/java/.../Application.java
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-dev.yml
│       └── application-prod.yml
├── platform-api/                           # API 层
│   ├── controller/                         # REST + SSE 端点
│   ├── dto/                                # 请求/响应 DTO
│   ├── advice/                             # 全局异常处理
│   └── interceptor/                        # 请求 ID/日志拦截器
├── platform-core/                          # 业务编排层
│   ├── query/                              # QueryService 编排
│   ├── ingestion/                          # IngestionService 编排
│   ├── agent/                              # AgentService 编排
│   ├── session/                            # 会话管理
│   └── analytics/                          # 统计服务
├── platform-rag/                           # RAG 核心算法（最有价值的模块）
│   ├── pipeline/                           # 检索流水线（责任链）
│   ├── retriever/                          # 向量/BM25 检索器
│   ├── fusion/                             # RRF 融合
│   ├── rerank/                             # 重排
│   ├── expansion/                          # 查询扩展
│   ├── splitter/                           # 文档分块
│   └── llm/                                # LLM 客户端封装
├── platform-infra/                         # 基础设施
│   ├── milvus/                             # Milvus 客户端
│   ├── es/                                 # ES 客户端
│   ├── postgres/                           # PG/MyBatis-Plus
│   ├── redis/                              # Redis 客户端
│   └── storage/                            # 文件存储抽象
├── platform-crawler/                       # iKnow 爬虫（保留原有功能）
└── platform-common/                        # 工具类
    ├── result/                             # 统一返回结构
    ├── exception/                          # 业务异常
    ├── util/                               # 工具类
    └── constant/                           # 常量
```

### 3.2 模块依赖关系（严格自上而下）

```
bootstrap → api → core → rag → infra → common
                    └────────→ infra ↗
                    └─→ crawler ─────↗
```

**关键纪律**：底层模块不能依赖上层模块。`infra` 不能用 `core` 的东西。这是未来能拆微服务的前提。

---

## 四、核心数据流设计

### 4.1 查询链路（同步 + SSE 双模式）

```
Controller
   ↓
QueryService（编排）
   ↓
QueryPipeline（责任链）
   ├─ QueryExpansionStep   → 同义词 + LLM 改写，产出 N 个查询变体
   ├─ ParallelRetrieveStep → 虚拟线程并行：
   │      ├─ MilvusRetriever（向量召回）
   │      └─ EsBm25Retriever（标题+正文 BM25）
   ├─ RrfFusionStep        → RRF 融合多路结果
   ├─ RerankStep           → SiliconFlow 重排（可关）
   ├─ ContextBuilderStep   → 拼 prompt，控 token 预算
   └─ LlmGenerateStep      → 流式生成

每步执行完发 ApplicationEvent → SseEventListener 推送给前端
```

### 4.2 入库链路

```
上传 / 爬取 → IngestionService
   ↓
DocumentParser（Tika 多格式解析）
   ↓
ContentHash 去重（PG 唯一索引）
   ↓
MarkdownChunker（按 ## ### 标题分块）
   ↓
EmbeddingClient 批量向量化
   ↓
并行写入：
   ├─ Milvus（chunks 集合）
   ├─ Milvus（full_doc 集合）
   └─ ES（标题+正文索引）
   ↓
PG 元数据表记录
```

---

## 五、Python → Java 核心功能映射

| Python 现状 | Java 方案 | 备注 |
|---|---|---|
| `retrieval_service.py` 多路并行检索 | LangChain4j `EmbeddingStoreContentRetriever` + 自写 `BM25Retriever` + 虚拟线程并行 | JDK 21 虚拟线程完美替代 ThreadPoolExecutor |
| 查询扩展（同义词+LLM） | `QueryTransformer` 接口 + 同义词字典（Caffeine 缓存） | LangChain4j 有现成 `CompressingQueryTransformer` |
| RRF 融合 | 手写 `ReciprocalRankFuser`，纯算法无依赖 | 30 行代码 |
| Cross-encoder 重排 | `ScoringModel` 调外部 SiliconFlow API | LangChain4j 已有 `ScoringModel` 抽象 |
| 标题 Jaccard/TF-IDF | **直接 ES BM25 替代** | 最大的简化点 |
| Agent ReAct | LangChain4j `AiServices` + `@Tool` 注解 | 比 Python 实现优雅 |
| 会话管理（30 分钟 TTL） | Redis + Spring Session 风格 | 分布式天然支持 |
| 查询统计（SQLite） | PostgreSQL + MyBatis-Plus | 顺手做成可视化 |
| 文档分块 | LangChain4j `DocumentSplitter` + 自定义 Markdown 标题分块 | 原项目的 MarkdownHeaderTextSplitter 逻辑直接移植 |
| MD5 去重 | DigestUtils + PG 唯一索引 | 一行 SQL 解决 |

---

## 六、关键技术点

### 6.1 虚拟线程做并行检索

```java
// JDK 21 虚拟线程，无线程池调优负担
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    var vector = scope.fork(() -> milvusRetriever.search(query));
    var bm25   = scope.fork(() -> esRetriever.search(query));
    scope.join().throwIfFailed();
    return rrfFuser.fuse(List.of(vector.get(), bm25.get()));
}
```

### 6.2 SSE 步骤事件解耦

```java
// 流水线步骤不直接持有 SseEmitter
@Component
public class RetrievalStepListener {
    @EventListener
    public void onStep(PipelineStepEvent e) {
        SseContext.current().send(e);
    }
}
```

**好处**：算法层不感知 SSE，单测好写。

### 6.3 配置强类型绑定

```yaml
rag:
  chunk:
    size: 2500
    overlap: 300
  retrieve:
    top-rough: 100
    top-final: 8
    weights:
      vector: 0.6
      bm25: 0.4
  rrf:
    k: 60
  rerank:
    enabled: true
    model: BAAI/bge-reranker-v2-m3
  cache:
    query-ttl-seconds: 300
```

对应 `@ConfigurationProperties(prefix = "rag")` 强类型类。

### 6.4 Agent 模式用 LangChain4j 原生能力

```java
@AiService
public interface ItSupportAgent {
    @SystemMessage("你是 IT 运维专家...")
    TokenStream chat(@MemoryId String sessionId, @UserMessage String question);

    @Tool("搜索 IT 知识库")
    String searchKnowledgeBase(String query);
}
```

比原 Python 实现简洁很多。

### 6.5 检索流水线（责任链 + 配置驱动）

```
QueryPipeline = QueryExpansion → ParallelRetrieve(向量+BM25)
              → RRFFusion → Rerank → ContextBuilder → LLMGenerate
```

每个阶段是独立 Bean，配置驱动开关（如 `rag.rerank.enabled=true`）。

---

## 七、数据库设计（PostgreSQL 核心表）

```sql
-- 文档元数据
CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(512) NOT NULL,
    content_hash CHAR(32) UNIQUE NOT NULL,  -- 去重
    file_path VARCHAR(1024),
    chunk_count INT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 多轮会话
CREATE TABLE sessions (
    id VARCHAR(64) PRIMARY KEY,
    messages JSONB NOT NULL,
    expires_at TIMESTAMP NOT NULL
);

-- 用户反馈
CREATE TABLE feedbacks (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(64),
    question TEXT,
    answer TEXT,
    rating SMALLINT,  -- 1 好评 / -1 差评
    comment TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 查询统计
CREATE TABLE query_logs (
    id BIGSERIAL PRIMARY KEY,
    question TEXT,
    duration_ms INT,
    source_count INT,
    cache_hit BOOLEAN,
    web_search_used BOOLEAN,
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_query_logs_created ON query_logs(created_at);
```

---

## 八、迁移路线（4 阶段，约 4 周）

| 阶段 | 周期 | 范围 | 验收标准 |
|---|---|---|---|
| **P1：骨架** | 第 1 周 | 多模块工程 + Compose 拉起 PG/Milvus/ES/Redis + 单条文档 ingest/query 跑通 | curl 上传一个 md，能查到答案 |
| **P2：核心 RAG** | 第 2 周 | 查询扩展 + 多路检索 + RRF + 重排 + SSE 步骤事件 | 检索质量与原 Python 版对齐（同样 20 个测试问题对比） |
| **P3：完整功能** | 第 3 周 | Agent 模式 + 会话 + 反馈 + 文档管理 + 统计 + 爬虫 | 接口覆盖率 100%，与原版功能等价 |
| **P4：前端 + 部署** | 第 4 周 | Vue 3 + TS 重做 + Docker Compose 整套 | 端到端可用，文档齐全 |

**关键原则**：每个阶段保留 Python 后端可启动，前端通过环境变量切换后端地址，便于对比验证。

---

## 九、风险与对策

| 风险 | 对策 |
|---|---|
| LangChain4j 中文支持有坑 | 关键算法（分块、查询扩展同义词）保留自实现，不全依赖框架 |
| 中文分词效果不如 Python | ES 用 IK 分词器，已是 Java 生态最佳方案 |
| 检索质量回归 | P2 阶段必须有自动化对比脚本，跑同一批问题对比新旧版本 |
| 向量化 API 调用成本 | 复用原项目的 batch + 内容哈希缓存策略 |
| Markdown 分块逻辑迁移 | 原 Python 的 MarkdownHeaderTextSplitter 算法直接移植 |
| Milvus 数据迁移 | 不从 Chroma 导数据，直接重新 ingest（源 .md 文件还在） |

---

## 十、可发散的拓展点（按需选择，不强求实现）

按价值排序：

1. **多租户隔离**：表加 `tenant_id`，Milvus 用 partition；适合企业内多部门
2. **多 Agent 协作**：路由 Agent → 检索 Agent / 代码 Agent / 工单 Agent
3. **混合检索权重在线调优**：A/B 测试不同权重对召回率的影响
4. **RAGAS 评估流水线**：定时检测知识库召回质量退化
5. **流式入库**：上传大文档时 SSE 推送分块进度
6. **训练数据闭环**：用户反馈 → 训练样本 → 微调小模型
7. **观测增强**：Spring Boot Actuator + Prometheus + Grafana
8. **知识图谱增强**：Neo4j 抽实体关系（**警惕**：复杂度高，没明确需求别上）

---

## 十一、警惕的过度设计

### 已剔除
- ❌ Spring Cloud 全家桶（Eureka / Gateway / Config Server / Nacos）
- ❌ 分布式微服务架构
- ❌ DDD 四层架构 + CQRS
- ❌ 自研 Embedding / Rerank 模型
- ❌ 引入 Kafka 做异步解耦（直接 `@Async` 或虚拟线程足够）
- ❌ 多级缓存（Caffeine + Redis 双层）——单 Redis 够用
- ❌ 自定义 Starter（除非要在多个项目复用）
- ❌ 一上来就上 K8s（开发期 Compose 足够）
- ❌ Service Mesh / Seata 分布式事务

### 何时才该拆微服务
出现下面**任意两个**信号再考虑：
- 团队超过 8 人，模块间协作变成瓶颈
- ingestion 跑批占满 CPU，影响在线 query 响应
- 算法团队想独立发版迭代 retrieval，不想等整个应用发布
- 单个 jar 启动时间超过 1 分钟
- 出现真实的多租户隔离需求（不同客户数据物理隔离）

---

## 十二、与原 Python 项目的功能对照清单

| 功能模块 | 原 Python 实现 | Java 方案 | 备注 |
|---|---|---|---|
| 文件上传 | `POST /upload` (FastAPI) | `POST /upload` (Spring MVC) | 接口契约保持一致 |
| 同步查询 | `POST /query` | `POST /query` | |
| SSE 流式查询 | `POST /query/stream` (sse-starlette) | `POST /query/stream` (SseEmitter) | |
| Agent 深度推理 | `POST /query/agent` | `POST /query/agent` | LangChain4j AiServices |
| 缓存清理 | `POST /cache/clear` | `POST /cache/clear` | |
| 反馈提交 | `POST /feedback` (JSONL) | `POST /feedback` (PG) | 改存数据库 |
| 文档列表 | `GET /documents` | `GET /documents` | MyBatis-Plus 分页 |
| 文档删除 | `DELETE /documents/{title}` | `DELETE /documents/{title}` | |
| 文档预览 | `GET /documents/{title}` | `GET /documents/{title}` | |
| 查询统计 | `GET /stats` (SQLite) | `GET /stats` (PG) | 改存 PG |
| 知识库爬取 | `cli/crawl_cli.py` | Spring Boot CLI Runner | `--crawl` 启动参数 |
| 批量入库 | `cli/upload_cli.py` | Spring Boot CLI Runner | `--ingest` 启动参数 |

---

## 附录 A：与现有方案的关系

本方案在以下文档基础上演进：
- `docs/RAG优化建议报告.md` - RAG 算法优化建议
- `docs/RAG过程可视化技术方案.md` - SSE 流式可视化方案
- `docs/前端优化建议报告.md` - 前端优化建议
- `docs/检索准确率优化建议报告.docx` - 检索准确率优化

本方案重点在**架构层面**重构，上述算法/可视化/前端优化点应在 P2/P4 阶段对应实现。

## 附录 B：决策记录

| 决策 | 备选方案 | 选择 | 理由 |
|---|---|---|---|
| 整体架构 | 单体 / 微服务 | **模块化单体** | 业务复杂度不够，微服务代价过高 |
| RAG 框架 | Spring AI / LangChain4j | **LangChain4j** | 更成熟，社区活跃 |
| 向量库 | Chroma / Milvus / Qdrant / pgvector / ES | **Milvus** | LangChain4j 原生支持，中文社区活跃 |
| 全文检索 | 自实现 BM25 / ES | **Elasticsearch** | 免去自实现，IK 分词成熟 |
| 流式实现 | WebFlux / SseEmitter | **SseEmitter** | 单体场景够用，比 WebFlux 简单 |
| ORM | JPA / MyBatis-Plus | **MyBatis-Plus** | 国内团队熟悉，灵活 |
| 前端 | 保留 Vue 3 / 重做 | **重做 Vue 3 + TS** | 补 TS，借机优化体验 |
