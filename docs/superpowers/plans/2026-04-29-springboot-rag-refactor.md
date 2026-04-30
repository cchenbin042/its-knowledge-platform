# Spring Boot RAG 知识平台重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将Python RAG知识平台完整迁移到Spring Boot模块化单体架构，保留全部核心功能。

**Architecture:** Maven多模块单体，严格分层依赖（bootstrap → api → core → rag → infra → common），虚拟线程并行检索，SSE流式响应，责任链查询流水线。

**Tech Stack:** Spring Boot 3.2.x + JDK 21 + LangChain4j 0.35+ + Milvus 2.4 + Elasticsearch 8 + PostgreSQL 16 + Redis 7 + MyBatis-Plus + Vue 3 + TypeScript

---

## Phase 1: 项目骨架搭建（第1周）

验收标准：curl上传一个md文件，能查到答案

---

### Task 1: 创建父POM与模块结构

**Files:**
- Create: `pom.xml`
- Create: `platform-bootstrap/pom.xml`
- Create: `platform-api/pom.xml`
- Create: `platform-core/pom.xml`
- Create: `platform-rag/pom.xml`
- Create: `platform-infra/pom.xml`
- Create: `platform-crawler/pom.xml`
- Create: `platform-common/pom.xml`

- [ ] **Step 1: 创建父POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.its</groupId>
    <artifactId>its-knowledge-platform</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>ITS Knowledge Platform</name>
    <description>企业级IT支持RAG知识问答平台</description>

    <modules>
        <module>platform-common</module>
        <module>platform-infra</module>
        <module>platform-rag</module>
        <module>platform-crawler</module>
        <module>platform-core</module>
        <module>platform-api</module>
        <module>platform-bootstrap</module>
    </modules>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        
        <spring-boot.version>3.2.5</spring-boot.version>
        <langchain4j.version>0.35.0</langchain4j.version>
        <mybatis-plus.version>3.5.5</mybatis-plus.version>
        <lombok.version>1.18.30</lombok.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            
            <dependency>
                <groupId>dev.langchain4j</groupId>
                <artifactId>langchain4j</artifactId>
                <version>${langchain4j.version}</version>
            </dependency>
            <dependency>
                <groupId>dev.langchain4j</groupId>
                <artifactId>langchain4j-open-ai</artifactId>
                <version>${langchain4j.version}</version>
            </dependency>
            
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>
            
            <!-- 内部模块 -->
            <dependency>
                <groupId>com.its</groupId>
                <artifactId>platform-common</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.its</groupId>
                <artifactId>platform-infra</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.its</groupId>
                <artifactId>platform-rag</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.its</groupId>
                <artifactId>platform-crawler</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.its</groupId>
                <artifactId>platform-core</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.its</groupId>
                <artifactId>platform-api</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <version>${spring-boot.version}</version>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.11.0</version>
                    <configuration>
                        <source>${java.version}</source>
                        <target>${java.version}</target>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

- [ ] **Step 2: 创建platform-common模块POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.its</groupId>
        <artifactId>its-knowledge-platform</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>platform-common</artifactId>
    <name>Platform Common</name>
    <description>工具类、常量、统一返回结构</description>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3: 创建platform-infra模块POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.its</groupId>
        <artifactId>its-knowledge-platform</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>platform-infra</artifactId>
    <name>Platform Infra</name>
    <description>基础设施：Milvus/ES/PG/Redis客户端</description>

    <dependencies>
        <dependency>
            <groupId>com.its</groupId>
            <artifactId>platform-common</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-milvus</artifactId>
            <version>${langchain4j.version}</version>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 4: 创建platform-rag模块POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.its</groupId>
        <artifactId>its-knowledge-platform</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>platform-rag</artifactId>
    <name>Platform RAG</name>
    <description>RAG核心算法：检索流水线、融合、重排、分块</description>

    <dependencies>
        <dependency>
            <groupId>com.its</groupId>
            <artifactId>platform-infra</artifactId>
        </dependency>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j</artifactId>
        </dependency>
        <dependency>
            <groupId>dev.langchain4j</groupId>
            <artifactId>langchain4j-open-ai</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 5: 创建platform-crawler模块POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.its</groupId>
        <artifactId>its-knowledge-platform</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>platform-crawler</artifactId>
    <name>Platform Crawler</name>
    <description>iKnow爬虫模块</description>

    <dependencies>
        <dependency>
            <groupId>com.its</groupId>
            <artifactId>platform-infra</artifactId>
        </dependency>
        <dependency>
            <groupId>org.jsoup</groupId>
            <artifactId>jsoup</artifactId>
            <version>1.17.2</version>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 6: 创建platform-core模块POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.its</groupId>
        <artifactId>its-knowledge-platform</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>platform-core</artifactId>
    <name>Platform Core</name>
    <description>业务编排层：Query/Ingestion/Agent/Session/Analytics</description>

    <dependencies>
        <dependency>
            <groupId>com.its</groupId>
            <artifactId>platform-rag</artifactId>
        </dependency>
        <dependency>
            <groupId>com.its</groupId>
            <artifactId>platform-crawler</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 7: 创建platform-api模块POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.its</groupId>
        <artifactId>its-knowledge-platform</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>platform-api</artifactId>
    <name>Platform API</name>
    <description>REST + SSE端点</description>

    <dependencies>
        <dependency>
            <groupId>com.its</groupId>
            <artifactId>platform-core</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 8: 创建platform-bootstrap模块POM**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.its</groupId>
        <artifactId>its-knowledge-platform</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>platform-bootstrap</artifactId>
    <name>Platform Bootstrap</name>
    <description>启动模块</description>

    <dependencies>
        <dependency>
            <groupId>com.its</groupId>
            <artifactId>platform-api</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 9: 验证Maven结构**

Run: `mvn dependency:tree -Dverbose=false`
Expected: 成功显示依赖树，无循环依赖

- [ ] **Step 10: Commit**

```bash
git add pom.xml platform-*/pom.xml
git commit -m "feat: 创建Maven多模块骨架结构"
```

---

### Task 2: 创建Docker Compose基础设施

**Files:**
- Create: `docker-compose.yml`
- Create: `init-scripts/init.sql`

- [ ] **Step 1: 创建Docker Compose文件**

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16
    container_name: its-postgres
    environment:
      POSTGRES_DB: its_knowledge
      POSTGRES_USER: its_user
      POSTGRES_PASSWORD: its_pass
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U its_user"]
      interval: 5s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: its-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 5s
      retries: 5

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.12.0
    container_name: its-es
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ports:
      - "9200:9200"
    volumes:
      - es_data:/usr/share/elasticsearch/data
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:9200/_cluster/health || exit 1"]
      interval: 10s
      timeout: 10s
      retries: 5

  milvus:
    image: milvusdb/milvus:v2.4-latest
    container_name: its-milvus
    ports:
      - "19530:19530"
    volumes:
      - milvus_data:/var/lib/milvus
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9091/healthz"]
      interval: 10s
      timeout: 10s
      retries: 5

volumes:
  postgres_data:
  redis_data:
  es_data:
  milvus_data:
```

- [ ] **Step 2: 创建数据库初始化脚本**

```sql
-- init-scripts/init.sql

-- 文档元数据
CREATE TABLE IF NOT EXISTS documents (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(512) NOT NULL,
    content_hash CHAR(32) UNIQUE NOT NULL,
    file_path VARCHAR(1024),
    chunk_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_documents_content_hash ON documents(content_hash);

-- 会话
CREATE TABLE IF NOT EXISTS sessions (
    id VARCHAR(64) PRIMARY KEY,
    messages JSONB NOT NULL DEFAULT '[]',
    created_at TIMESTAMP DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sessions_expires_at ON sessions(expires_at);

-- 用户反馈
CREATE TABLE IF NOT EXISTS feedbacks (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(64),
    question TEXT,
    answer TEXT,
    rating SMALLINT,
    comment TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_feedbacks_created_at ON feedbacks(created_at);

-- 查询日志
CREATE TABLE IF NOT EXISTS query_logs (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(64),
    question TEXT,
    duration_ms INT,
    source_count INT,
    cache_hit BOOLEAN DEFAULT FALSE,
    web_search_used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_query_logs_created_at ON query_logs(created_at);
```

- [ ] **Step 3: 启动基础设施**

Run: `docker-compose up -d`
Expected: 所有容器启动成功，健康检查通过

- [ ] **Step 4: 验证数据库连接**

Run: `docker exec its-postgres psql -U its_user -d its_knowledge -c "\dt"`
Expected: 显示documents, sessions, feedbacks, query_logs四张表

- [ ] **Step 5: Commit**

```bash
git add docker-compose.yml init-scripts/
git commit -m "feat: 添加Docker Compose基础设施配置"
```

---

### Task 3: 创建platform-common基础类

**Files:**
- Create: `platform-common/src/main/java/com/its/platform/common/result/Result.java`
- Create: `platform-common/src/main/java/com/its/platform/common/result/ResultCode.java`
- Create: `platform-common/src/main/java/com/its/platform/common/exception/BusinessException.java`
- Create: `platform-common/src/main/java/com/its/platform/common/constant/RagConstants.java`

- [ ] **Step 1: 创建统一返回结构**

```java
// platform-common/src/main/java/com/its/platform/common/result/Result.java
package com.its.platform.common.result;

import lombok.Data;
import java.io.Serializable;

@Data
public class Result<T> implements Serializable {
    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(ResultCode.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return fail(resultCode.getCode(), resultCode.getMessage());
    }
}
```

- [ ] **Step 2: 创建返回码枚举**

```java
// platform-common/src/main/java/com/its/platform/common/result/ResultCode.java
package com.its.platform.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    
    // 业务错误码
    DOCUMENT_NOT_FOUND(1001, "文档不存在"),
    DOCUMENT_ALREADY_EXISTS(1002, "文档已存在"),
    EMBEDDING_FAILED(1003, "向量化失败"),
    RETRIEVE_FAILED(1004, "检索失败"),
    SESSION_EXPIRED(1005, "会话已过期");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

- [ ] **Step 3: 创建业务异常类**

```java
// platform-common/src/main/java/com/its/platform/common/exception/BusinessException.java
package com.its.platform.common.exception;

import com.its.platform.common.result.ResultCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final Integer code;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
```

- [ ] **Step 4: 创建RAG常量类**

```java
// platform-common/src/main/java/com/its/platform/common/constant/RagConstants.java
package com.its.platform.common.constant;

public final class RagConstants {
    public static final String COLLECTION_CHUNKS = "chunks";
    public static final String COLLECTION_FULL_DOC = "full_doc";
    
    public static final String ES_INDEX_NAME = "its_knowledge";
    
    public static final int DEFAULT_TOP_K = 8;
    public static final int DEFAULT_RRF_K = 60;
    
    public static final int SESSION_TTL_SECONDS = 1800; // 30分钟
    
    private RagConstants() {}
}
```

- [ ] **Step 5: Commit**

```bash
git add platform-common/src/
git commit -m "feat: 创建common模块基础类"
```

---

### Task 4: 创建启动类与配置文件

**Files:**
- Create: `platform-bootstrap/src/main/java/com/its/platform/Application.java`
- Create: `platform-bootstrap/src/main/resources/application.yml`
- Create: `platform-bootstrap/src/main/resources/application-dev.yml`

- [ ] **Step 1: 创建启动类**

```java
// platform-bootstrap/src/main/java/com/its/platform/Application.java
package com.its.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

- [ ] **Step 2: 创建主配置文件**

```yaml
# platform-bootstrap/src/main/resources/application.yml
server:
  port: 8080

spring:
  application:
    name: its-knowledge-platform
  profiles:
    active: dev

  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/its_knowledge
    username: its_user
    password: its_pass

  data:
    redis:
      host: localhost
      port: 6379

    elasticsearch:
      cluster-nodes: localhost:9200

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true

# RAG配置
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
    enabled: false
    model: BAAI/bge-reranker-v2-m3
  cache:
    query-ttl-seconds: 300

# LLM配置
llm:
  base-url: ${LLM_BASE_URL:http://localhost:8000}
  api-key: ${LLM_API_KEY:sk-placeholder}
  embedding-model: ${LLM_EMBEDDING_MODEL:text-embedding-3-small}
  chat-model: ${LLM_CHAT_MODEL:gpt-4o-mini}
```

- [ ] **Step 3: 创建开发环境配置**

```yaml
# platform-bootstrap/src/main/resources/application-dev.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/its_knowledge
    username: its_user
    password: its_pass

  data:
    redis:
      host: localhost
      port: 6379

  data:
    elasticsearch:
      cluster-nodes: localhost:9200

logging:
  level:
    com.its: DEBUG
    dev.langchain4j: DEBUG
```

- [ ] **Step 4: 验证启动**

Run: `mvn clean package -DskipTests && java -jar platform-bootstrap/target/platform-bootstrap-1.0.0-SNAPSHOT.jar`
Expected: Spring Boot启动成功，端口8080监听

- [ ] **Step 5: Commit**

```bash
git add platform-bootstrap/src/
git commit -m "feat: 创建启动类与配置文件"
```

---

### Task 5: 创建Milvus向量库客户端

**Files:**
- Create: `platform-infra/src/main/java/com/its/platform/infra/milvus/MilvusConfig.java`
- Create: `platform-infra/src/main/java/com/its/platform/infra/milvus/MilvusVectorStore.java`

- [ ] **Step 1: 创建Milvus配置类**

```java
// platform-infra/src/main/java/com/its/platform/infra/milvus/MilvusConfig.java
package com.its.platform.infra.milvus;

import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
public class MilvusConfig {
    
    @Value("${milvus.host:localhost}")
    private String host;
    
    @Value("${milvus.port:19530}")
    private int port;
    
    @Value("${rag.embedding-dimension:1536}")
    private int embeddingDimension;

    @Bean
    public MilvusClientV2 milvusClient() {
        MilvusClientV2 client = new MilvusClientV2(
            io.milvus.v2.client.ConnectParam.builder()
                .uri("http://" + host + ":" + port)
                .build()
        );
        
        initCollections(client);
        return client;
    }

    private void initCollections(MilvusClientV2 client) {
        createCollectionIfNotExists(client, "chunks", embeddingDimension);
        createCollectionIfNotExists(client, "full_doc", embeddingDimension);
        log.info("Milvus collections initialized");
    }

    private void createCollectionIfNotExists(MilvusClientV2 client, String collectionName, int dimension) {
        HasCollectionReq hasReq = HasCollectionReq.builder()
            .collectionName(collectionName)
            .build();
        
        if (!client.hasCollection(hasReq)) {
            CreateCollectionReq createReq = CreateCollectionReq.builder()
                .collectionName(collectionName)
                .dimension(dimension)
                .dataType(DataType.FloatVector)
                .build();
            client.createCollection(createReq);
            log.info("Created Milvus collection: {}", collectionName);
        }
    }
}
```

- [ ] **Step 2: 创建向量存储服务**

```java
// platform-infra/src/main/java/com/its/platform/infra/milvus/MilvusVectorStore.java
package com.its.platform.infra.milvus;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.InsertResp;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MilvusVectorStore implements EmbeddingStore<TextSegment> {

    private final MilvusClientV2 milvusClient;
    private final String collectionName = "chunks";

    @Override
    public String add(Embedding embedding) {
        return addAll(List.of(embedding)).get(0);
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        List<FloatVec> vectors = embeddings.stream()
            .map(e -> new FloatVec(e.vector()))
            .collect(Collectors.toList());
        
        InsertReq insertReq = InsertReq.builder()
            .collectionName(collectionName)
            .data(vectors)
            .build();
        
        InsertResp resp = milvusClient.insert(insertReq);
        return resp.getDataWrapper().getIds();
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> segments) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < embeddings.size(); i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("embedding", embeddings.get(i).vector());
            row.put("text", segments.get(i).text());
            row.put("document_id", segments.get(i).metadata("document_id"));
            rows.add(row);
        }
        
        InsertReq insertReq = InsertReq.builder()
            .collectionName(collectionName)
            .data(rows)
            .build();
        
        InsertResp resp = milvusClient.insert(insertReq);
        return resp.getDataWrapper().getIds();
    }

    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        FloatVec queryVec = new FloatVec(request.queryEmbedding().vector());
        
        SearchReq searchReq = SearchReq.builder()
            .collectionName(collectionName)
            .data(List.of(queryVec))
            .topK(request.maxResults())
            .outputFields(List.of("text", "document_id"))
            .build();
        
        SearchResp resp = milvusClient.search(searchReq);
        
        List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
        for (SearchResp.SearchResult result : resp.getSearchResults()) {
            List<SearchResp.SearchResultData> data = result.getData();
            for (SearchResp.SearchResultData item : data) {
                double score = item.getScore();
                String text = (String) item.getEntity().get("text");
                String documentId = (String) item.getEntity().get("document_id");
                
                TextSegment segment = TextSegment.from(text, 
                    dev.langchain4j.data.document.Metadata.from("document_id", documentId));
                
                matches.add(new EmbeddingMatch<>(score, 
                    item.getId().toString(), segment, null));
            }
        }
        
        return new EmbeddingSearchResult<>(matches);
    }

    public void deleteByDocumentId(String documentId) {
        // 删除指定文档的所有向量
        log.info("Deleting vectors for document: {}", documentId);
        // Milvus delete by expression
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add platform-infra/src/main/java/com/its/platform/infra/milvus/
git commit -m "feat: 创建Milvus向量库客户端"
```

---

### Task 6: 创建Elasticsearch BM25检索器

**Files:**
- Create: `platform-infra/src/main/java/com/its/platform/infra/es/EsConfig.java`
- Create: `platform-infra/src/main/java/com/its/platform/infra/es/EsBm25Retriever.java`
- Create: `platform-infra/src/main/java/com/its/platform/infra/es/EsDocumentIndexer.java`

- [ ] **Step 1: 创建ES配置类**

```java
// platform-infra/src/main/java/com/its/platform/infra/es/EsConfig.java
package com.its.platform.infra.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EsConfig {
    
    @Value("${spring.data.elasticsearch.cluster-nodes:localhost:9200}")
    private String clusterNodes;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        HttpHost host = HttpHost.create(clusterNodes);
        RestClient restClient = RestClient.builder(host).build();
        
        ElasticsearchTransport transport = new RestClientTransport(
            restClient, new JacksonJsonpMapper());
        
        return new ElasticsearchClient(transport);
    }
}
```

- [ ] **Step 2: 创建BM25检索器**

```java
// platform-infra/src/main/java/com/its/platform/infra/es/EsBm25Retriever.java
package com.its.platform.infra.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EsBm25Retriever {

    private final ElasticsearchClient esClient;
    private final String indexName = "its_knowledge";

    public List<SearchResult> search(String query, int topK) {
        try {
            Query bm25Query = Query.of(q -> q
                .multiMatch(m -> m
                    .fields("title^2", "content")
                    .query(query)
                    .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
                )
            );

            SearchRequest searchReq = SearchRequest.of(s -> s
                .index(indexName)
                .query(bm25Query)
                .size(topK)
            );

            SearchResponse<EsDocument> response = esClient.search(searchReq, EsDocument.class);
            
            List<SearchResult> results = new ArrayList<>();
            for (Hit<EsDocument> hit : response.hits().hits()) {
                EsDocument doc = hit.source();
                if (doc != null) {
                    results.add(new SearchResult(
                        doc.documentId(),
                        doc.title(),
                        doc.content(),
                        hit.score() != null ? hit.score() : 0.0
                    ));
                }
            }
            
            log.info("BM25 search returned {} results for query: {}", results.size(), query);
            return results;
            
        } catch (Exception e) {
            log.error("BM25 search failed", e);
            return List.of();
        }
    }

    public record SearchResult(String documentId, String title, String content, double score) {}
    public record EsDocument(String documentId, String title, String content) {}
}
```

- [ ] **Step 3: 创建文档索引器**

```java
// platform-infra/src/main/java/com/its/platform/infra/es/EsDocumentIndexer.java
package com.its.platform.infra.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class EsDocumentIndexer {

    private final ElasticsearchClient esClient;
    private final String indexName = "its_knowledge";

    public void ensureIndexExists() throws IOException {
        boolean exists = esClient.indices().exists(e -> e.index(indexName)).value();
        
        if (!exists) {
            CreateIndexRequest createReq = CreateIndexRequest.of(c -> c
                .index(indexName)
                .mappings(m -> m
                    .properties("title", p -> p.text(t -> t.analyzer("ik_max_word")))
                    .properties("content", p -> p.text(t -> t.analyzer("ik_max_word")))
                    .properties("document_id", p -> p.keyword(k -> k))
                )
            );
            
            esClient.indices().create(createReq);
            log.info("Created ES index: {}", indexName);
        }
    }

    public void indexDocument(String documentId, String title, String content) throws IOException {
        ensureIndexExists();
        
        EsBm25Retriever.EsDocument doc = new EsBm25Retriever.EsDocument(documentId, title, content);
        
        esClient.index(i -> i
            .index(indexName)
            .id(documentId)
            .document(doc)
        );
        
        log.info("Indexed document: {} in ES", documentId);
    }

    public void deleteDocument(String documentId) throws IOException {
        esClient.delete(d -> d.index(indexName).id(documentId));
        log.info("Deleted document: {} from ES", documentId);
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add platform-infra/src/main/java/com/its/platform/infra/es/
git commit -m "feat: 创建Elasticsearch BM25检索器"
```

---

### Task 7: 创建PostgreSQL数据访问层

**Files:**
- Create: `platform-infra/src/main/java/com/its/platform/infra/postgres/entity/DocumentEntity.java`
- Create: `platform-infra/src/main/java/com/its/platform/infra/postgres/entity/SessionEntity.java`
- Create: `platform-infra/src/main/java/com/its/platform/infra/postgres/mapper/DocumentMapper.java`
- Create: `platform-infra/src/main/java/com/its/platform/infra/postgres/mapper/SessionMapper.java`
- Create: `platform-infra/src/main/java/com/its/platform/infra/postgres/repository/DocumentRepository.java`

- [ ] **Step 1: 创建Document实体**

```java
// platform-infra/src/main/java/com/its/platform/infra/postgres/entity/DocumentEntity.java
package com.its.platform.infra.postgres.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("documents")
public class DocumentEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String title;
    private String contentHash;
    private String filePath;
    private Integer chunkCount;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 2: 创建Session实体**

```java
// platform-infra/src/main/java/com/its/platform/infra/postgres/entity/SessionEntity.java
package com.its.platform.infra.postgres.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "sessions", autoResultMap = true)
public class SessionEntity {
    @TableId(type = IdType.INPUT)
    private String id;
    
    private List<MessageItem> messages;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageItem {
        private String role;
        private String content;
        private LocalDateTime timestamp;
    }
}
```

- [ ] **Step 3: 创建Mapper接口**

```java
// platform-infra/src/main/java/com/its/platform/infra/postgres/mapper/DocumentMapper.java
package com.its.platform.infra.postgres.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.its.platform.infra.postgres.entity.DocumentEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DocumentMapper extends BaseMapper<DocumentEntity> {
}
```

```java
// platform-infra/src/main/java/com/its/platform/infra/postgres/mapper/SessionMapper.java
package com.its.platform.infra.postgres.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.its.platform.infra.postgres.entity.SessionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SessionMapper extends BaseMapper<SessionEntity> {
}
```

- [ ] **Step 4: 创建DocumentRepository**

```java
// platform-infra/src/main/java/com/its/platform/infra/postgres/repository/DocumentRepository.java
package com.its.platform.infra.postgres.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.its.platform.infra.postgres.entity.DocumentEntity;
import com.its.platform.infra.postgres.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DocumentRepository {

    private final DocumentMapper documentMapper;

    public DocumentEntity save(DocumentEntity document) {
        documentMapper.insert(document);
        return document;
    }

    public Optional<DocumentEntity> findById(Long id) {
        return Optional.ofNullable(documentMapper.selectById(id));
    }

    public Optional<DocumentEntity> findByTitle(String title) {
        LambdaQueryWrapper<DocumentEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentEntity::getTitle, title);
        return Optional.ofNullable(documentMapper.selectOne(wrapper));
    }

    public Optional<DocumentEntity> findByContentHash(String contentHash) {
        LambdaQueryWrapper<DocumentEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentEntity::getContentHash, contentHash);
        return Optional.ofNullable(documentMapper.selectOne(wrapper));
    }

    public List<DocumentEntity> findAll() {
        return documentMapper.selectList(null);
    }

    public void deleteByTitle(String title) {
        LambdaQueryWrapper<DocumentEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentEntity::getTitle, title);
        documentMapper.delete(wrapper);
    }

    public boolean existsByContentHash(String contentHash) {
        LambdaQueryWrapper<DocumentEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentEntity::getContentHash, contentHash);
        return documentMapper.selectCount(wrapper) > 0;
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add platform-infra/src/main/java/com/its/platform/infra/postgres/
git commit -m "feat: 创建PostgreSQL数据访问层"
```

---

### Task 8: 创建文档上传基础功能

**Files:**
- Create: `platform-rag/src/main/java/com/its/platform/rag/splitter/MarkdownChunker.java`
- Create: `platform-rag/src/main/java/com/its/platform/rag/splitter/ChunkResult.java`
- Create: `platform-core/src/main/java/com/its/platform/core/ingestion/IngestionService.java`
- Create: `platform-api/src/main/java/com/its/platform/api/controller/DocumentController.java`

- [ ] **Step 1: 创建Markdown分块器**

```java
// platform-rag/src/main/java/com/its/platform/rag/splitter/ChunkResult.java
package com.its.platform.rag.splitter;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChunkResult {
    private String text;
    private String heading;
    private int startIndex;
    private int endIndex;
}
```

```java
// platform-rag/src/main/java/com/its/platform/rag/splitter/MarkdownChunker.java
package com.its.platform.rag.splitter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class MarkdownChunker {

    private static final Pattern HEADER_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);
    
    private final int chunkSize = 2500;
    private final int overlap = 300;

    public List<ChunkResult> chunk(String markdownContent, String documentTitle) {
        List<ChunkResult> chunks = new ArrayList<>();
        
        // 按标题分块
        Matcher matcher = HEADER_PATTERN.matcher(markdownContent);
        List<Integer> headerPositions = new ArrayList<>();
        List<String> headerTitles = new ArrayList<>();
        
        headerPositions.add(0);
        headerTitles.add(documentTitle);
        
        while (matcher.find()) {
            headerPositions.add(matcher.start());
            headerTitles.add(matcher.group(2).trim());
        }
        headerPositions.add(markdownContent.length());
        
        // 根据标题位置分割内容
        for (int i = 0; i < headerPositions.size() - 1; i++) {
            int start = headerPositions.get(i);
            int end = headerPositions.get(i + 1);
            String heading = headerTitles.get(i);
            String content = markdownContent.substring(start, end).trim();
            
            if (content.length() > chunkSize) {
                // 大块继续分割
                splitLargeChunk(content, heading, chunks);
            } else if (!content.isEmpty()) {
                chunks.add(ChunkResult.builder()
                    .text(content)
                    .heading(heading)
                    .startIndex(start)
                    .endIndex(end)
                    .build());
            }
        }
        
        log.info("Chunked document into {} chunks", chunks.size());
        return chunks;
    }

    private void splitLargeChunk(String content, String heading, List<ChunkResult> chunks) {
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());
            String chunkText = content.substring(start, end);
            
            chunks.add(ChunkResult.builder()
                .text(chunkText)
                .heading(heading)
                .startIndex(start)
                .endIndex(end)
                .build());
            
            start = end - overlap;
            if (start < 0) start = 0;
        }
    }
}
```

- [ ] **Step 2: 创建入库服务**

```java
// platform-core/src/main/java/com/its/platform/core/ingestion/IngestionService.java
package com.its.platform.core.ingestion;

import com.its.platform.infra.es.EsDocumentIndexer;
import com.its.platform.infra.milvus.MilvusVectorStore;
import com.its.platform.infra.postgres.entity.DocumentEntity;
import com.its.platform.infra.postgres.repository.DocumentRepository;
import com.its.platform.rag.splitter.ChunkResult;
import com.its.platform.rag.splitter.MarkdownChunker;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {

    private final DocumentRepository documentRepository;
    private final MarkdownChunker markdownChunker;
    private final EmbeddingModel embeddingModel;
    private final MilvusVectorStore milvusVectorStore;
    private final EsDocumentIndexer esDocumentIndexer;

    public DocumentEntity ingest(MultipartFile file) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        String filename = file.getOriginalFilename();
        String title = filename != null ? filename.replace(".md", "") : "untitled";
        
        // 计算内容哈希
        String contentHash = hashContent(content);
        
        // 去重检查
        if (documentRepository.existsByContentHash(contentHash)) {
            log.warn("Document already exists: {}", title);
            throw new RuntimeException("Document already exists");
        }
        
        // 分块
        List<ChunkResult> chunks = markdownChunker.chunk(content, title);
        
        // 向量化并存储
        List<TextSegment> segments = chunks.stream()
            .map(c -> TextSegment.from(c.getText(), 
                dev.langchain4j.data.document.Metadata.from("document_id", title)))
            .collect(Collectors.toList());
        
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        milvusVectorStore.addAll(embeddings, segments);
        
        // ES索引
        esDocumentIndexer.indexDocument(title, title, content);
        
        // PG记录
        DocumentEntity document = DocumentEntity.builder()
            .title(title)
            .contentHash(contentHash)
            .filePath(filename)
            .chunkCount(chunks.size())
            .createdAt(LocalDateTime.now())
            .build();
        
        documentRepository.save(document);
        
        log.info("Ingested document: {} with {} chunks", title, chunks.size());
        return document;
    }

    private String hashContent(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash content", e);
        }
    }
}
```

- [ ] **Step 3: 创建文档Controller**

```java
// platform-api/src/main/java/com/its/platform/api/controller/DocumentController.java
package com.its.platform.api.controller;

import com.its.platform.common.result.Result;
import com.its.platform.core.ingestion.IngestionService;
import com.its.platform.infra.postgres.entity.DocumentEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final IngestionService ingestionService;
    private final DocumentRepository documentRepository;

    @PostMapping("/upload")
    public Result<DocumentEntity> upload(@RequestParam("file") MultipartFile file) throws IOException {
        DocumentEntity document = ingestionService.ingest(file);
        return Result.success(document);
    }

    @GetMapping
    public Result<List<DocumentEntity>> list() {
        List<DocumentEntity> documents = documentRepository.findAll();
        return Result.success(documents);
    }

    @GetMapping("/{title}")
    public Result<DocumentEntity> get(@PathVariable String title) {
        return documentRepository.findByTitle(title)
            .map(Result::success)
            .orElse(Result.fail(ResultCode.NOT_FOUND));
    }

    @DeleteMapping("/{title}")
    public Result<Void> delete(@PathVariable String title) {
        documentRepository.deleteByTitle(title);
        return Result.success();
    }
}
```

- [ ] **Step 4: 修复Controller依赖**

需要将DocumentRepository和ResultCode引入api模块的可见范围。

- [ ] **Step 5: Commit**

```bash
git add platform-rag/src/main/java/com/its/platform/rag/splitter/ \
       platform-core/src/main/java/com/its/platform/core/ingestion/ \
       platform-api/src/main/java/com/its/platform/api/controller/
git commit -m "feat: 创建文档上传入库功能"
```

---

### Task 9: 创建基础查询功能

**Files:**
- Create: `platform-rag/src/main/java/com/its/platform/rag/retriever/RetrievalResult.java`
- Create: `platform-rag/src/main/java/com/its/platform/rag/retriever/CompositeRetriever.java`
- Create: `platform-core/src/main/java/com/its/platform/core/query/QueryService.java`
- Create: `platform-api/src/main/java/com/its/platform/api/controller/QueryController.java`

- [ ] **Step 1: 创建检索结果结构**

```java
// platform-rag/src/main/java/com/its/platform/rag/retriever/RetrievalResult.java
package com.its.platform.rag.retriever;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RetrievalResult {
    private String documentId;
    private String title;
    private String content;
    private double score;
    private String source; // "vector" or "bm25"
    
    public static List<RetrievalResult> fromVectorMatches(List<EmbeddingMatch<TextSegment>> matches) {
        return matches.stream()
            .map(m -> RetrievalResult.builder()
                .documentId(m.embeddingId())
                .content(m.embedded().text())
                .score(m.score())
                .source("vector")
                .build())
            .collect(Collectors.toList());
    }
    
    public static List<RetrievalResult> fromBm25Results(List<EsBm25Retriever.SearchResult> results) {
        return results.stream()
            .map(r -> RetrievalResult.builder()
                .documentId(r.documentId())
                .title(r.title())
                .content(r.content())
                .score(r.score())
                .source("bm25")
                .build())
            .collect(Collectors.toList());
    }
}
```

- [ ] **Step 2: 创建组合检索器**

```java
// platform-rag/src/main/java/com/its/platform/rag/retriever/CompositeRetriever.java
package com.its.platform.rag.retriever;

import com.its.platform.infra.es.EsBm25Retriever;
import com.its.platform.infra.milvus.MilvusVectorStore;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.StructuredTaskScope;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompositeRetriever {

    private final MilvusVectorStore milvusVectorStore;
    private final EsBm25Retriever esBm25Retriever;
    private final EmbeddingModel embeddingModel;
    
    private final int topK = 100;

    public List<RetrievalResult> retrieve(String query) {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            // 虚拟线程并行检索
            var vectorTask = scope.fork(() -> retrieveVector(query));
            var bm25Task = scope.fork(() -> retrieveBm25(query));
            
            scope.join().throwIfFailed();
            
            List<RetrievalResult> vectorResults = vectorTask.get();
            List<RetrievalResult> bm25Results = bm25Task.get();
            
            log.info("Vector: {}, BM25: {} results", vectorResults.size(), bm25Results.size());
            
            List<RetrievalResult> allResults = new ArrayList<>();
            allResults.addAll(vectorResults);
            allResults.addAll(bm25Results);
            
            return allResults;
            
        } catch (Exception e) {
            log.error("Parallel retrieval failed", e);
            return List.of();
        }
    }

    private List<RetrievalResult> retrieveVector(String query) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        
        EmbeddingSearchRequest searchReq = EmbeddingSearchRequest.builder()
            .queryEmbedding(queryEmbedding)
            .maxResults(topK)
            .build();
        
        EmbeddingSearchResult<TextSegment> result = milvusVectorStore.search(searchReq);
        
        return RetrievalResult.fromVectorMatches(result.matches());
    }

    private List<RetrievalResult> retrieveBm25(String query) {
        List<EsBm25Retriever.SearchResult> results = esBm25Retriever.search(query, topK);
        return RetrievalResult.fromBm25Results(results);
    }
}
```

- [ ] **Step 3: 创建QueryService**

```java
// platform-core/src/main/java/com/its/platform/core/query/QueryService.java
package com.its.platform.core.query;

import com.its.platform.rag.retriever.CompositeRetriever;
import com.its.platform.rag.retriever.RetrievalResult;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryService {

    private final CompositeRetriever compositeRetriever;
    private final ChatLanguageModel chatModel;

    public QueryResponse query(String question) {
        // 检索
        List<RetrievalResult> results = compositeRetriever.retrieve(question);
        
        // 取前N个构建context
        List<RetrievalResult> topResults = results.stream()
            .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
            .limit(8)
            .collect(Collectors.toList());
        
        String context = buildContext(topResults);
        
        // 生成答案
        String prompt = buildPrompt(question, context);
        String answer = chatModel.generate(prompt);
        
        log.info("Query completed: {} sources", topResults.size());
        
        return QueryResponse.builder()
            .question(question)
            .answer(answer)
            .sources(topResults)
            .build();
    }

    private String buildContext(List<RetrievalResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("参考文档：\n\n");
        for (int i = 0; i < results.size(); i++) {
            RetrievalResult r = results.get(i);
            sb.append(String.format("[%d] %s\n%s\n\n", i + 1, r.getTitle(), r.getContent()));
        }
        return sb.toString();
    }

    private String buildPrompt(String question, String context) {
        return String.format(
            "你是一个IT运维专家。根据以下参考文档回答用户问题。\n\n" +
            "%s\n\n" +
            "用户问题：%s\n\n" +
            "请基于参考文档给出准确、详细的回答。如果文档中没有相关信息，请说明。",
            context, question
        );
    }
}
```

```java
// platform-core/src/main/java/com/its/platform/core/query/QueryResponse.java
package com.its.platform.core.query;

import com.its.platform.rag.retriever.RetrievalResult;
import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
public class QueryResponse {
    private String question;
    private String answer;
    private List<RetrievalResult> sources;
}
```

- [ ] **Step 4: 创建QueryController**

```java
// platform-api/src/main/java/com/its/platform/api/controller/QueryController.java
package com.its.platform.api.controller;

import com.its.platform.common.result.Result;
import com.its.platform.core.query.QueryResponse;
import com.its.platform.core.query.QueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/query")
@RequiredArgsConstructor
public class QueryController {

    private final QueryService queryService;

    @PostMapping
    public Result<QueryResponse> query(@RequestBody QueryRequest request) {
        QueryResponse response = queryService.query(request.getQuestion());
        return Result.success(response);
    }
}
```

```java
// platform-api/src/main/java/com/its/platform/api/dto/QueryRequest.java
package com.its.platform.api.dto;

import lombok.Data;

@Data
public class QueryRequest {
    private String question;
}
```

- [ ] **Step 5: Commit**

```bash
git add platform-rag/src/main/java/com/its/platform/rag/retriever/ \
       platform-core/src/main/java/com/its/platform/core/query/ \
       platform-api/src/main/java/com/its/platform/api/controller/ \
       platform-api/src/main/java/com/its/platform/api/dto/
git commit -m "feat: 创建基础查询功能"
```

---

### Task 10: Phase 1验收测试

**Files:**
- None (验证步骤)

- [ ] **Step 1: 启动完整应用**

Run: `docker-compose up -d && mvn clean package -DskipTests && java -jar platform-bootstrap/target/platform-bootstrap-1.0.0-SNAPSHOT.jar`
Expected: 应用启动成功，连接PG/Milvus/ES/Redis

- [ ] **Step 2: 上传测试文档**

准备测试文档 `test.md`:
```markdown
# IT运维知识库

## 服务器故障排查

当服务器出现故障时，首先检查：
1. CPU使用率是否过高
2. 内存是否不足
3. 磁盘空间是否充足
4. 网络连接是否正常

## 常见问题解答

### CPU使用率过高怎么办？

检查是否有异常进程占用CPU，使用top命令查看进程列表。如果是Java应用，检查是否有死循环或内存泄漏。
```

Run: `curl -X POST -F "file=@test.md" http://localhost:8080/documents/upload`
Expected: 返回文档信息，包含id、title、chunkCount

- [ ] **Step 3: 执行查询**

Run: `curl -X POST -H "Content-Type: application/json" -d '{"question":"CPU使用率过高怎么办"}' http://localhost:8080/query`
Expected: 返回答案和sources列表

- [ ] **Step 4: Phase 1完成**

验收标准达成：curl上传md文件，能查到答案。

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: Phase 1完成 - 项目骨架搭建验收通过"
```

---

## Phase 2: 核心RAG功能（第2周）

验收标准：检索质量与原Python版对齐（同样20个测试问题对比）

---

### Task 11: 创建RRF融合算法

**Files:**
- Create: `platform-rag/src/main/java/com/its/platform/rag/fusion/RrfFusion.java`

- [ ] **Step 1: 实现RRF融合**

```java
// platform-rag/src/main/java/com/its/platform/rag/fusion/RrfFusion.java
package com.its.platform.rag.fusion;

import com.its.platform.rag.retriever.RetrievalResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RrfFusion {

    private final int k = 60; // RRF常数

    public List<RetrievalResult> fuse(List<List<RetrievalResult>> rankedLists) {
        // 按documentId聚合分数
        Map<String, Double> scoreMap = new HashMap<>();
        Map<String, RetrievalResult> resultMap = new HashMap<>();
        
        for (List<RetrievalResult> list : rankedLists) {
            for (int rank = 0; rank < list.size(); rank++) {
                RetrievalResult result = list.get(rank);
                String key = result.getDocumentId() + "_" + result.getContent().hashCode();
                
                double rrfScore = 1.0 / (k + rank + 1);
                scoreMap.merge(key, rrfScore, Double::sum);
                resultMap.put(key, result);
            }
        }
        
        // 按融合分数排序
        List<RetrievalResult> fused = scoreMap.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .map(e -> {
                RetrievalResult r = resultMap.get(e.getKey());
                r.setScore(e.getValue());
                return r;
            })
            .collect(Collectors.toList());
        
        log.info("RRF fused {} results from {} lists", fused.size(), rankedLists.size());
        return fused;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add platform-rag/src/main/java/com/its/platform/rag/fusion/
git commit -m "feat: 实现RRF融合算法"
```

---

### Task 12: 创建查询扩展模块

**Files:**
- Create: `platform-rag/src/main/java/com/its/platform/rag/expansion/SynonymExpander.java`
- Create: `platform-rag/src/main/java/com/its/platform/rag/expansion/QueryExpansionResult.java`

- [ ] **Step 1: 创建同义词扩展器**

```java
// platform-rag/src/main/java/com/its/platform/rag/expansion/QueryExpansionResult.java
package com.its.platform.rag.expansion;

import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
public class QueryExpansionResult {
    private String originalQuery;
    private List<String> expandedQueries;
}
```

```java
// platform-rag/src/main/java/com/its/platform/rag/expansion/SynonymExpander.java
package com.its.platform.rag.expansion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class SynonymExpander {

    private static final Map<String, List<String>> SYNONYMS = Map.of(
        "CPU", List.of("处理器", "中央处理器", "cpu"),
        "内存", List.of("RAM", "memory", "内存条"),
        "磁盘", List.of("硬盘", "disk", "storage", "存储"),
        "服务器", List.of("主机", "server", "机器"),
        "网络", List.of("network", "网路", "连接"),
        "故障", List.of("问题", "error", "异常", "错误"),
        "排查", List.of("诊断", "troubleshoot", "检查", "分析")
    );

    private static final Pattern WORD_PATTERN = Pattern.compile("\\w+|[\\u4e00-\\u9fa5]+");

    public QueryExpansionResult expand(String query) {
        List<String> expansions = new ArrayList<>();
        expansions.add(query); // 原查询
        
        Matcher matcher = WORD_PATTERN.matcher(query);
        List<String> words = new ArrayList<>();
        while (matcher.find()) {
            words.add(matcher.group());
        }
        
        // 对每个词找同义词，生成变体
        for (String word : words) {
            List<String> syns = SYNONYMS.get(word);
            if (syns != null && !syns.isEmpty()) {
                for (String syn : syns) {
                    String expanded = query.replace(word, syn);
                    expansions.add(expanded);
                }
            }
        }
        
        // 最多保留5个变体
        List<String> limited = expansions.stream()
            .distinct()
            .limit(5)
            .collect(Collectors.toList());
        
        log.info("Expanded query '{}' to {} variants", query, limited.size());
        
        return QueryExpansionResult.builder()
            .originalQuery(query)
            .expandedQueries(limited)
            .build();
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add platform-rag/src/main/java/com/its/platform/rag/expansion/
git commit -m "feat: 实现同义词查询扩展"
```

---

### Task 13: 创建查询流水线框架

**Files:**
- Create: `platform-rag/src/main/java/com/its/platform/rag/pipeline/QueryPipeline.java`
- Create: `platform-rag/src/main/java/com/its/platform/rag/pipeline/PipelineStep.java`
- Create: `platform-rag/src/main/java/com/its/platform/rag/pipeline/PipelineContext.java`
- Create: `platform-rag/src/main/java/com/its/platform/rag/pipeline/PipelineStepEvent.java`

- [ ] **Step 1: 创建流水线基础设施**

```java
// platform-rag/src/main/java/com/its/platform/rag/pipeline/PipelineStep.java
package com.its.platform.rag.pipeline;

public interface PipelineStep {
    String name();
    void execute(PipelineContext context);
    boolean isEnabled();
}
```

```java
// platform-rag/src/main/java/com/its/platform/rag/pipeline/PipelineContext.java
package com.its.platform.rag.pipeline;

import com.its.platform.rag.expansion.QueryExpansionResult;
import com.its.platform.rag.retriever.RetrievalResult;
import lombok.Data;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;

@Data
public class PipelineContext {
    private String originalQuery;
    private List<String> expandedQueries = new ArrayList<>();
    private List<RetrievalResult> retrievalResults = new ArrayList<>();
    private List<RetrievalResult> fusedResults = new ArrayList<>();
    private List<RetrievalResult> rerankedResults = new ArrayList<>();
    private String contextText;
    private String answer;
    
    private ApplicationEventPublisher eventPublisher;
    
    public void publishStep(String stepName, Object data) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new PipelineStepEvent(stepName, data));
        }
    }
}
```

```java
// platform-rag/src/main/java/com/its/platform/rag/pipeline/PipelineStepEvent.java
package com.its.platform.rag.pipeline;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

@Getter
public class PipelineStepEvent extends ApplicationEvent {
    private final String stepName;
    private final Object data;
    private final LocalDateTime timestamp;

    public PipelineStepEvent(String stepName, Object data) {
        super(stepName);
        this.stepName = stepName;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }
}
```

- [ ] **Step 2: 创建流水线编排器**

```java
// platform-rag/src/main/java/com/its/platform/rag/pipeline/QueryPipeline.java
package com.its.platform.rag.pipeline;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueryPipeline {

    private final List<PipelineStep> steps;
    private final ApplicationEventPublisher eventPublisher;

    public PipelineContext execute(String query) {
        PipelineContext context = new PipelineContext();
        context.setOriginalQuery(query);
        context.setEventPublisher(eventPublisher);
        
        log.info("Starting query pipeline for: {}", query);
        
        for (PipelineStep step : steps) {
            if (step.isEnabled()) {
                log.info("Executing step: {}", step.name());
                try {
                    step.execute(context);
                    context.publishStep(step.name(), context);
                } catch (Exception e) {
                    log.error("Step {} failed", step.name(), e);
                    throw e;
                }
            } else {
                log.info("Skipping disabled step: {}", step.name());
            }
        }
        
        log.info("Query pipeline completed");
        return context;
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add platform-rag/src/main/java/com/its/platform/rag/pipeline/
git commit -m "feat: 创建查询流水线框架"
```

---

### Task 14: 实现流水线各步骤

**Files:**
- Create: `platform-rag/src/main/java/com/its/platform/rag/pipeline/steps/QueryExpansionStep.java`
- Create: `platform-rag/src/main/java/com/its/platform/rag/pipeline/steps/ParallelRetrieveStep.java`
- Create: `platform-rag/src/main/java/com/its/platform/rag/pipeline/steps/RrfFusionStep.java`
- Create: `platform-rag/src/main/java/com/its/platform/rag/pipeline/steps/ContextBuilderStep.java`
- Create: `platform-rag/src/main/java/com/its/platform/rag/pipeline/steps/LlmGenerateStep.java`

- [ ] **Step 1: 查询扩展步骤**

```java
// platform-rag/src/main/java/com/its/platform/rag/pipeline/steps/QueryExpansionStep.java
package com.its.platform.rag.pipeline.steps;

import com.its.platform.rag.expansion.QueryExpansionResult;
import com.its.platform.rag.expansion.SynonymExpander;
import com.its.platform.rag.pipeline.PipelineContext;
import com.its.platform.rag.pipeline.PipelineStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class QueryExpansionStep implements PipelineStep {

    private final SynonymExpander synonymExpander;

    @Override
    public String name() {
        return "query_expansion";
    }

    @Override
    public void execute(PipelineContext context) {
        QueryExpansionResult result = synonymExpander.expand(context.getOriginalQuery());
        context.setExpandedQueries(result.getExpandedQueries());
        log.info("Expanded to {} queries", result.getExpandedQueries().size());
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
```

- [ ] **Step 2: 并行检索步骤**

```java
// platform-rag/src/main/java/com/its/platform/rag/pipeline/steps/ParallelRetrieveStep.java
package com.its.platform.rag.pipeline.steps;

import com.its.platform.rag.retriever.CompositeRetriever;
import com.its.platform.rag.retriever.RetrievalResult;
import com.its.platform.rag.pipeline.PipelineContext;
import com.its.platform.rag.pipeline.PipelineStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.StructuredTaskScope;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class ParallelRetrieveStep implements PipelineStep {

    private final CompositeRetriever compositeRetriever;

    @Override
    public String name() {
        return "parallel_retrieve";
    }

    @Override
    public void execute(PipelineContext context) {
        List<String> queries = context.getExpandedQueries();
        List<RetrievalResult> allResults = new ArrayList<>();
        
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            List<java.util.concurrent.Future<List<RetrievalResult>>> futures = new ArrayList<>();
            
            for (String query : queries) {
                futures.add(scope.fork(() -> compositeRetriever.retrieve(query)));
            }
            
            scope.join().throwIfFailed();
            
            for (var future : futures) {
                allResults.addAll(future.get());
            }
        } catch (Exception e) {
            log.error("Parallel retrieval failed", e);
            throw new RuntimeException(e);
        }
        
        context.setRetrievalResults(allResults);
        log.info("Retrieved {} results from {} queries", allResults.size(), queries.size());
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
```

- [ ] **Step 3: RRF融合步骤**

```java
// platform-rag/src/main/java/com/its/platform/rag/pipeline/steps/RrfFusionStep.java
package com.its.platform.rag.pipeline.steps;

import com.its.platform.rag.fusion.RrfFusion;
import com.its.platform.rag.retriever.RetrievalResult;
import com.its.platform.rag.pipeline.PipelineContext;
import com.its.platform.rag.pipeline.PipelineStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class RrfFusionStep implements PipelineStep {

    private final RrfFusion rrfFusion;
    private final int topFinal = 8;

    @Override
    public String name() {
        return "rrf_fusion";
    }

    @Override
    public void execute(PipelineContext context) {
        // 按source分组
        List<List<RetrievalResult>> grouped = context.getRetrievalResults().stream()
            .collect(Collectors.groupingBy(RetrievalResult::getSource))
            .values().stream()
            .collect(Collectors.toList());
        
        List<RetrievalResult> fused = rrfFusion.fuse(grouped);
        
        // 取top K
        List<RetrievalResult> topResults = fused.stream()
            .limit(topFinal)
            .collect(Collectors.toList());
        
        context.setFusedResults(topResults);
        log.info("Fused to {} results", topResults.size());
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
```

- [ ] **Step 4: Context构建步骤**

```java
// platform-rag/src/main/java/com/its/platform/rag/pipeline/steps/ContextBuilderStep.java
package com.its.platform.rag.pipeline.steps;

import com.its.platform.rag.retriever.RetrievalResult;
import com.its.platform.rag.pipeline.PipelineContext;
import com.its.platform.rag.pipeline.PipelineStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Order(4)
public class ContextBuilderStep implements PipelineStep {

    @Override
    public String name() {
        return "context_builder";
    }

    @Override
    public void execute(PipelineContext context) {
        List<RetrievalResult> results = context.getFusedResults();
        
        StringBuilder sb = new StringBuilder();
        sb.append("以下是参考文档内容：\n\n");
        
        for (int i = 0; i < results.size(); i++) {
            RetrievalResult r = results.get(i);
            sb.append(String.format("【文档%d】\n%s\n\n", i + 1, r.getContent()));
        }
        
        String contextText = sb.toString();
        
        // Token预算控制（简化版：字符数）
        int maxChars = 8000;
        if (contextText.length() > maxChars) {
            contextText = contextText.substring(0, maxChars) + "...";
        }
        
        context.setContextText(contextText);
        log.info("Built context with {} chars", contextText.length());
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
```

- [ ] **Step 5: LLM生成步骤**

```java
// platform-rag/src/main/java/com/its/platform/rag/pipeline/steps/LlmGenerateStep.java
package com.its.platform.rag.pipeline.steps;

import com.its.platform.rag.pipeline.PipelineContext;
import com.its.platform.rag.pipeline.PipelineStep;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(5)
@RequiredArgsConstructor
public class LlmGenerateStep implements PipelineStep {

    private final ChatLanguageModel chatModel;

    @Override
    public String name() {
        return "llm_generate";
    }

    @Override
    public void execute(PipelineContext context) {
        String prompt = buildPrompt(context);
        
        String answer = chatModel.generate(prompt);
        context.setAnswer(answer);
        
        log.info("Generated answer: {} chars", answer.length());
    }

    private String buildPrompt(PipelineContext context) {
        return String.format(
            "你是一个IT运维专家。请根据以下参考文档回答用户问题。\n\n" +
            "%s\n\n" +
            "用户问题：%s\n\n" +
            "要求：\n" +
            "1. 基于参考文档给出准确回答\n" +
            "2. 如果文档中没有相关信息，明确说明\n" +
            "3. 回答要详细、专业\n" +
            "4. 在回答末尾标注引用的文档编号",
            context.getContextText(),
            context.getOriginalQuery()
        );
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add platform-rag/src/main/java/com/its/platform/rag/pipeline/steps/
git commit -m "feat: 实现查询流水线各步骤"
```

---

### Task 15: 创建SSE流式响应

**Files:**
- Create: `platform-api/src/main/java/com/its/platform/api/controller/SseQueryController.java`
- Create: `platform-api/src/main/java/com/its/platform/api/sse/SseEventListener.java`

- [ ] **Step 1: 创建SSE Controller**

```java
// platform-api/src/main/java/com/its/platform/api/controller/SseQueryController.java
package com.its.platform.api.controller;

import com.its.platform.rag.pipeline.PipelineContext;
import com.its.platform.rag.pipeline.QueryPipeline;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/query")
@RequiredArgsConstructor
public class SseQueryController {

    private final QueryPipeline queryPipeline;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @PostMapping("/stream")
    public SseEmitter streamQuery(@RequestBody QueryRequest request) {
        SseEmitter emitter = new SseEmitter(60000L); // 60秒超时
        
        executor.execute(() -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("start")
                    .data("开始处理查询..."));
                
                PipelineContext context = queryPipeline.execute(request.getQuestion());
                
                emitter.send(SseEmitter.event()
                    .name("complete")
                    .data(context));
                
                emitter.complete();
            } catch (IOException e) {
                log.error("SSE error", e);
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }
}
```

- [ ] **Step 2: 创建SSE事件监听器**

```java
// platform-api/src/main/java/com/its/platform/api/sse/SseEventListener.java
package com.its.platform.api.sse;

import com.its.platform.rag.pipeline.PipelineStepEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseEventListener {

    private final Map<String, SseEmitter> activeEmitters = new ConcurrentHashMap<>();

    public void register(String sessionId, SseEmitter emitter) {
        activeEmitters.put(sessionId, emitter);
        emitter.onCompletion(() -> activeEmitters.remove(sessionId));
    }

    @EventListener
    public void onPipelineStep(PipelineStepEvent event) {
        // 广播给所有活跃的SSE连接
        activeEmitters.forEach((id, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                    .name(event.getStepName())
                    .data(event.getData()));
            } catch (IOException e) {
                log.warn("Failed to send SSE event to {}", id);
                activeEmitters.remove(id);
            }
        });
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add platform-api/src/main/java/com/its/platform/api/controller/ \
       platform-api/src/main/java/com/its/platform/api/sse/
git commit -m "feat: 创建SSE流式响应功能"
```

---

### Task 16: Phase 2验收测试

**Files:**
- None

- [ ] **Step 1: 准备20个测试问题**

创建测试问题文件 `test-questions.txt`：
```
CPU使用率过高怎么办
服务器内存不足怎么排查
磁盘空间满了怎么处理
网络连接异常怎么诊断
...
```

- [ ] **Step 2: 运行对比测试**

对比Python版本和Java版本的检索结果质量。

- [ ] **Step 3: 记录对比结果**

确保检索质量与Python版对齐。

- [ ] **Step 4: Commit**

```bash
git commit --allow-empty -m "feat: Phase 2完成 - 核心RAG功能验收通过"
```

---

## Phase 3: 完整功能（第3周）

验收标准：接口覆盖率100%，与原版功能等价

---

### Task 17-30: 完整功能实现

（此阶段任务包括：Agent模式、会话管理、反馈收集、文档管理、统计分析、爬虫模块等）

---

## Phase 4: 前端与部署（第4周）

验收标准：端到端可用，文档齐全

---

### Task 31-40: Vue3前端与Docker部署

（此阶段任务包括：Vue3 TS前端重构、Docker Compose完善、文档编写等）

---

## 附录：类型一致性检查

在实施过程中需确保：
- `RetrievalResult` 的字段命名在各模块保持一致
- `PipelineContext` 的方法签名与各Step的调用匹配
- Controller的DTO与Service的返回类型匹配