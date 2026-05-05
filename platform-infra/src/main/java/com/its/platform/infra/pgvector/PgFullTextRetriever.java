package com.its.platform.infra.pgvector;

import com.its.platform.infra.postgres.repository.ChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * PostgreSQL full-text retriever using tsvector + tsquery
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PgFullTextRetriever {

    private final ChunkRepository chunkRepository;

    public List<SearchResult> search(String query, int limit) {
        List<ChunkRepository.SearchResult> results = chunkRepository.searchByFullText(query, limit);

        return results.stream()
                .map(this::toSearchResult)
                .collect(Collectors.toList());
    }

    private SearchResult toSearchResult(ChunkRepository.SearchResult r) {
        return new SearchResult(
                String.valueOf(r.id()),
                String.valueOf(r.documentId()),
                r.content(),
                r.score() != null ? r.score() : 0.0
        );
    }

    public record SearchResult(
            String documentId,
            String title,
            String content,
            double score
    ) {}
}