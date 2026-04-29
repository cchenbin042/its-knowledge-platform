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
                    .properties("title", p -> p.text(t -> t.analyzer("standard")))
                    .properties("content", p -> p.text(t -> t.analyzer("standard")))
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