package com.its.platform.rag.retriever;

import com.its.platform.infra.es.EsBm25Retriever;
import com.its.platform.infra.milvus.MilvusVectorStore;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.data.segment.TextSegment;
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
            // Parallel retrieval with virtual threads
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