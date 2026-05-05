package com.its.platform.rag.retriever;

import com.its.platform.infra.pgvector.PgFullTextRetriever;
import com.its.platform.rag.fusion.RrfFusion;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
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

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final PgFullTextRetriever pgFullTextRetriever;
    private final EmbeddingModel embeddingModel;
    private final RrfFusion rrfFusion;

    private final int topK = 100;

    public List<RetrievalResult> retrieve(String query) {
        // Use subtask as virtual thread to handle failures independently
        var vectorResults = new ArrayList<RetrievalResult>();
        var fullTextResults = new ArrayList<RetrievalResult>();

        try (var scope = new StructuredTaskScope<>()) {
            var vectorTask = scope.fork(() -> {
                try {
                    return retrieveVector(query);
                } catch (Exception e) {
                    log.warn("Vector retrieval failed: {}", e.getMessage());
                    return List.<RetrievalResult>of();
                }
            });
            var fullTextTask = scope.fork(() -> {
                try {
                    return retrieveFullText(query);
                } catch (Exception e) {
                    log.warn("Full-text retrieval failed: {}", e.getMessage());
                    return List.<RetrievalResult>of();
                }
            });

            scope.join();

            vectorResults.addAll(vectorTask.get());
            fullTextResults.addAll(fullTextTask.get());

        } catch (Exception e) {
            log.error("Parallel retrieval failed", e);
        }

        log.info("Vector: {}, FullText: {} results", vectorResults.size(), fullTextResults.size());

        if (vectorResults.isEmpty() && fullTextResults.isEmpty()) {
            return List.of();
        }

        // If only one source has results, return those directly
        if (vectorResults.isEmpty()) {
            return fullTextResults.stream().limit(8).collect(java.util.stream.Collectors.toList());
        }
        if (fullTextResults.isEmpty()) {
            return vectorResults.stream().limit(8).collect(java.util.stream.Collectors.toList());
        }

        // Apply RRF fusion when both have results
        List<RetrievalResult> fusedResults = rrfFusion.fuse(vectorResults, fullTextResults);
        log.info("RRF fusion completed: {} results", fusedResults.size());
        return fusedResults;
    }

    private List<RetrievalResult> retrieveVector(String query) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();

        EmbeddingSearchRequest searchReq = EmbeddingSearchRequest.builder()
            .queryEmbedding(queryEmbedding)
            .maxResults(topK)
            .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchReq);

        return RetrievalResult.fromVectorMatches(result.matches());
    }

    private List<RetrievalResult> retrieveFullText(String query) {
        List<PgFullTextRetriever.SearchResult> results = pgFullTextRetriever.search(query, topK);
        return RetrievalResult.fromFullTextResults(results);
    }
}