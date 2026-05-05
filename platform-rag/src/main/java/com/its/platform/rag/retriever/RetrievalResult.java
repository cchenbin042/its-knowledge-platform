package com.its.platform.rag.retriever;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.data.segment.TextSegment;
import com.its.platform.infra.pgvector.PgFullTextRetriever;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RetrievalResult {
    private String documentId;
    private String title;
    private String content;
    private double score;
    private String source;

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

    public static List<RetrievalResult> fromFullTextResults(List<PgFullTextRetriever.SearchResult> results) {
        return results.stream()
            .map(r -> RetrievalResult.builder()
                .documentId(String.valueOf(r.documentId()))
                .content(r.content())
                .score(r.score())
                .source("fulltext")
                .build())
            .collect(Collectors.toList());
    }
}