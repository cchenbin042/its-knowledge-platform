package com.its.platform.infra.milvus;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.InsertResp;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MilvusVectorStore implements EmbeddingStore<TextSegment> {

    private final MilvusClientV2 milvusClient;
    private final String collectionName = "chunks";

    @Override
    public String add(Embedding embedding) {
        return addAll(List.of(embedding)).get(0);
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        List<FloatVec> vectors = embeddings.stream()
            .map(e -> new FloatVec(e.vector()))
            .collect(Collectors.toList());

        InsertReq insertReq = InsertReq.builder()
            .collectionName(collectionName)
            .data(vectors)
            .build();

        InsertResp resp = milvusClient.insert(insertReq);
        return resp.getDataWrapper().getIds();
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> segments) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < embeddings.size(); i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("embedding", embeddings.get(i).vector());
            row.put("text", segments.get(i).text());
            row.put("document_id", segments.get(i).metadata("document_id"));
            rows.add(row);
        }

        InsertReq insertReq = InsertReq.builder()
            .collectionName(collectionName)
            .data(rows)
            .build();

        InsertResp resp = milvusClient.insert(insertReq);
        return resp.getDataWrapper().getIds();
    }

    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        FloatVec queryVec = new FloatVec(request.queryEmbedding().vector());

        SearchReq searchReq = SearchReq.builder()
            .collectionName(collectionName)
            .data(List.of(queryVec))
            .topK(request.maxResults())
            .outputFields(List.of("text", "document_id"))
            .build();

        SearchResp resp = milvusClient.search(searchReq);

        List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
        for (SearchResp.SearchResult result : resp.getSearchResults()) {
            List<SearchResp.SearchResultData> data = result.getData();
            for (SearchResp.SearchResultData item : data) {
                double score = item.getScore();
                String text = (String) item.getEntity().get("text");
                String documentId = (String) item.getEntity().get("document_id");

                TextSegment segment = TextSegment.from(text,
                    dev.langchain4j.data.document.Metadata.from("document_id", documentId));

                matches.add(new EmbeddingMatch<>(score,
                    item.getId().toString(), segment, null));
            }
        }

        return new EmbeddingSearchResult<>(matches);
    }

    public void deleteByDocumentId(String documentId) {
        log.info("Deleting vectors for document: {}", documentId);
    }
}