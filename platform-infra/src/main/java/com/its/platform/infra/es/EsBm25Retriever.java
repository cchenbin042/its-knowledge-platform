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