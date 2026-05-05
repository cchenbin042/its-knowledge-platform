package com.its.platform.infra.postgres.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.its.platform.infra.postgres.entity.ChunkEntity;
import com.its.platform.infra.pgvector.VectorTypeHandler;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;

import java.util.List;
import java.time.LocalDateTime;

/**
 * 分块 Mapper，提供向量检索和全文检索方法
 */
@Mapper
public interface ChunkMapper extends BaseMapper<ChunkEntity> {

    /**
     * 插入分块（带向量）
     * 使用原生 SQL 正确处理 vector 类型
     */
    @Insert("INSERT INTO chunks (document_id, chunk_index, content, embedding, created_at) " +
            "VALUES (#{documentId}, #{chunkIndex}, #{content}, " +
            "#{embedding,typeHandler=com.its.platform.infra.pgvector.VectorTypeHandler}::vector, #{createdAt})")
    void insertWithVector(ChunkEntity chunk);

    /**
     * 向量相似度检索（cosine distance）
     * <=> 是 pgvector 的 cosine distance 运算符
     * 返回 cosine distance 作为 score，用于后续计算相似度
     */
    @Select("SELECT id, document_id, chunk_index, content, created_at, " +
            "(embedding <=> #{embedding,typeHandler=com.its.platform.infra.pgvector.VectorTypeHandler}::vector) as score " +
            "FROM chunks " +
            "WHERE embedding IS NOT NULL " +
            "ORDER BY embedding <=> #{embedding,typeHandler=com.its.platform.infra.pgvector.VectorTypeHandler}::vector " +
            "LIMIT #{limit}")
    List<ChunkEntityWithScore> searchByVector(@Param("embedding") float[] embedding, @Param("limit") int limit);

    /**
     * 全文检索（BM25 风格排名）
     * 使用 tsvector + tsquery，通过 ts_rank_bm25 函数计算得分
     */
    @Select("SELECT id, document_id, chunk_index, content, " +
            "created_at, " +
            "ts_rank(content_tsv, plainto_tsquery('simple', #{query})) as score " +
            "FROM chunks " +
            "WHERE content_tsv @@ plainto_tsquery('simple', #{query}) " +
            "ORDER BY ts_rank(content_tsv, plainto_tsquery('simple', #{query})) DESC " +
            "LIMIT #{limit}")
    List<ChunkEntityWithScore> searchByFullText(@Param("query") String query, @Param("limit") int limit);

    /**
     * 按文档 ID 删除所有分块
     */
    @Delete("DELETE FROM chunks WHERE document_id = #{documentId}")
    int deleteByDocumentId(@Param("documentId") Long documentId);

    /**
     * 带得分的结果（用于全文检索）
     */
    interface ChunkEntityWithScore {
        Long getId();
        Long getDocumentId();
        Integer getChunkIndex();
        String getContent();
        LocalDateTime getCreatedAt();
        Double getScore();
    }
}