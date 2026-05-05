package com.its.platform.infra.pgvector;

import com.its.platform.infra.postgres.entity.ChunkEntity;
import com.its.platform.infra.postgres.repository.ChunkRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * pgvector 向量存储实现，适配 LangChain4j EmbeddingStore 接口
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PgvectorVectorStore implements EmbeddingStore<TextSegment> {

    private final ChunkRepository chunkRepository;

    /**
     * 添加单个向量嵌入（带指定 ID）
     */
    @Override
    public void add(String id, Embedding embedding) {
        // pgvector 使用自增 ID，传入的 id 仅用于日志
        add(embedding);
    }

    /**
     * 添加单个向量嵌入（无关联文本）
     */
    @Override
    public String add(Embedding embedding) {
        return addAll(List.of(embedding)).get(0);
    }

    /**
     * 添加单个向量嵌入（带关联文本）
     */
    @Override
    public String add(Embedding embedding, TextSegment segment) {
        return addAll(List.of(embedding), List.of(segment)).get(0);
    }

    /**
     * 批量添加向量嵌入（无关联文本）
     */
    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        List<TextSegment> emptySegments = new ArrayList<>();
        for (int i = 0; i < embeddings.size(); i++) {
            emptySegments.add(TextSegment.from(""));
        }
        return addAll(embeddings, emptySegments);
    }

    /**
     * 批量添加向量嵌入（带关联文本段）
     * 这是主要的入库方法，用于文档分块入库
     */
    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> segments) {
        if (embeddings.size() != segments.size()) {
            throw new IllegalArgumentException("Embeddings and segments must have same size");
        }

        List<ChunkEntity> chunks = new ArrayList<>();

        for (int i = 0; i < embeddings.size(); i++) {
            Embedding embedding = embeddings.get(i);
            TextSegment segment = segments.get(i);

            // 从 metadata 获取 documentId 和 chunkIndex
            Metadata metadata = segment.metadata();
            Long documentId = metadata.getLong("documentId");
            if (documentId == null) {
                documentId = 0L;
            }

            Integer chunkIndex = metadata.getInteger("chunkIndex");
            if (chunkIndex == null) {
                chunkIndex = i;
            }

            ChunkEntity chunk = ChunkEntity.builder()
                    .documentId(documentId)
                    .chunkIndex(chunkIndex)
                    .content(segment.text())
                    .embedding(toFloatArray(embedding))
                    .createdAt(LocalDateTime.now())
                    .build();

            chunks.add(chunk);
        }

        chunkRepository.saveAll(chunks);

        // 返回生成的 ID
        List<String> generatedIds = new ArrayList<>();
        for (ChunkEntity chunk : chunks) {
            generatedIds.add(String.valueOf(chunk.getId()));
        }

        log.info("Added {} embeddings with segments", embeddings.size());
        return generatedIds;
    }

    /**
     * 向量相似度检索
     */
    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        float[] queryVector = toFloatArray(request.queryEmbedding());
        Integer maxResults = request.maxResults();
        int limit = maxResults != null ? maxResults : 10;

        List<ChunkRepository.SearchResult> results = chunkRepository.searchByVector(queryVector, limit);

        List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
        for (ChunkRepository.SearchResult r : results) {
            // cosine distance -> similarity score (1 - distance)
            double score = 1.0 - (r.score() != null ? r.score() : 1.0);

            Double minScore = request.minScore();
            if (minScore != null && score < minScore) {
                continue;
            }

            Metadata metadata = Metadata.from("documentId", r.documentId());
            metadata.put("chunkIndex", r.chunkIndex());

            TextSegment segment = TextSegment.from(r.content(), metadata);

            matches.add(new EmbeddingMatch<>(
                    score,
                    String.valueOf(r.id()),
                    Embedding.from(queryVector), // 不返回原始 embedding，使用查询向量占位
                    segment
            ));
        }

        log.info("Vector search returned {} matches", matches.size());
        return new EmbeddingSearchResult<>(matches);
    }

    /**
     * 将 LangChain4j Embedding 转换为 float 数组
     */
    private float[] toFloatArray(Embedding embedding) {
        float[] result = new float[embedding.dimension()];
        for (int i = 0; i < embedding.dimension(); i++) {
            result[i] = (float) embedding.vector()[i];
        }
        return result;
    }
}