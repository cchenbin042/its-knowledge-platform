-- init-scripts/init.sql

-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

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

-- 文档分块表（向量存储）
CREATE TABLE IF NOT EXISTS chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1024),  -- BGE-M3 向量维度 1024
    content_tsv tsvector GENERATED ALWAYS AS (to_tsvector('simple', content)) STORED,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 向量索引 (HNSW)
CREATE INDEX IF NOT EXISTS idx_chunks_embedding ON chunks USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);

-- 全文检索索引 (GIN)
CREATE INDEX IF NOT EXISTS idx_chunks_content_tsv ON chunks USING gin(content_tsv);

-- 文档-分块关联索引
CREATE INDEX IF NOT EXISTS idx_chunks_document_id ON chunks(document_id);

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
    rating INTEGER,  -- 1-5 scale (1=very bad, 5=very good)
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

-- 全文检索函数（BM25 风格排名）
CREATE OR REPLACE FUNCTION ts_rank_bm25(tsvector, tsquery) RETURNS float AS $$
BEGIN
    RETURN ts_rank($1, $2, 32);  -- 使用 normalization 32 (document length)
END;
$$ LANGUAGE plpgsql IMMUTABLE;