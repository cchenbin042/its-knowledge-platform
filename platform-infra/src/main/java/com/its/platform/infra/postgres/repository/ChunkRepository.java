package com.its.platform.infra.postgres.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.its.platform.infra.postgres.entity.ChunkEntity;
import com.its.platform.infra.postgres.mapper.ChunkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * 分块 Repository，封装向量存储和检索操作
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ChunkRepository {

    private final ChunkMapper chunkMapper;

    /**
     * 批量保存分块
     * 没有向量时使用普通 insert，有向量时使用自定义 SQL
     */
    public void saveAll(List<ChunkEntity> chunks) {
        if (chunks.isEmpty()) return;
        for (ChunkEntity chunk : chunks) {
            if (chunk.getEmbedding() != null && chunk.getEmbedding().length > 0) {
                chunkMapper.insertWithVector(chunk);
            } else {
                chunkMapper.insert(chunk);
            }
        }
        log.info("Saved {} chunks", chunks.size());
    }

    /**
     * 向量相似度检索
     *
     * @param embedding 查询向量
     * @param limit     返回数量
     * @return 相似度排序的分块列表（带得分）
     */
    public List<SearchResult> searchByVector(float[] embedding, int limit) {
        if (embedding == null || embedding.length == 0) {
            return List.of();
        }

        try {
            List<ChunkMapper.ChunkEntityWithScore> results = chunkMapper.searchByVector(embedding, limit);

            if (results == null) {
                log.warn("Vector search returned null for embedding dimension: {}", embedding.length);
                return List.of();
            }

            List<SearchResult> searchResults = new ArrayList<>();
            for (ChunkMapper.ChunkEntityWithScore r : results) {
                if (r == null) continue;
                searchResults.add(new SearchResult(
                        r.getId(),
                        r.getDocumentId(),
                        r.getChunkIndex(),
                        r.getContent(),
                        r.getScore()
                ));
            }

            log.info("Vector search returned {} results (embedding dim: {})", searchResults.size(), embedding.length);
            return searchResults;
        } catch (Exception e) {
            log.error("Vector search failed for embedding dimension: {}", embedding.length, e);
            return List.of();
        }
    }

    /**
     * 全文检索
     *
     * @param query  tsquery 格式的查询字符串
     * @param limit  返回数量
     * @return 按得分排序的结果列表
     */
    public List<SearchResult> searchByFullText(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        try {
            List<ChunkMapper.ChunkEntityWithScore> results = chunkMapper.searchByFullText(query, limit);

            if (results == null) {
                log.warn("Full-text search returned null for query: {}", query);
                return List.of();
            }

            List<SearchResult> searchResults = new ArrayList<>();
            for (ChunkMapper.ChunkEntityWithScore r : results) {
                if (r == null) continue;
                searchResults.add(new SearchResult(
                        r.getId(),
                        r.getDocumentId(),
                        r.getChunkIndex(),
                        r.getContent(),
                        r.getScore()
                ));
            }

            log.info("Full-text search returned {} results for query: {}", searchResults.size(), query);
            return searchResults;
        } catch (Exception e) {
            log.error("Full-text search failed for query: {}", query, e);
            return List.of();
        }
    }

    /**
     * 按文档 ID 删除所有分块
     *
     * @param documentId 文档 ID
     */
    public void deleteByDocumentId(Long documentId) {
        int deleted = chunkMapper.deleteByDocumentId(documentId);
        log.info("Deleted {} chunks for document {}", deleted, documentId);
    }

    /**
     * 按文档 ID 查询所有分块
     *
     * @param documentId 文档 ID
     * @return 分块列表
     */
    public List<ChunkEntity> findByDocumentId(Long documentId) {
        LambdaQueryWrapper<ChunkEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChunkEntity::getDocumentId, documentId)
                .orderByAsc(ChunkEntity::getChunkIndex);
        return chunkMapper.selectList(wrapper);
    }

    /**
     * 全文检索结果
     */
    public record SearchResult(
            Long id,
            Long documentId,
            Integer chunkIndex,
            String content,
            Double score
    ) {}
}