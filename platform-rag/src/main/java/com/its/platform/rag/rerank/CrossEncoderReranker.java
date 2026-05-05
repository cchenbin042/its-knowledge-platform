package com.its.platform.rag.rerank;

import com.its.platform.rag.retriever.RetrievalResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Cross-encoder reranker using external API.
 * Calls a rerank service (e.g., SiliconFlow, Cohere) to re-score results.
 */
@Slf4j
@Component
public class CrossEncoderReranker {

    @Value("${rag.rerank.enabled:false}")
    private boolean enabled;

    @Value("${rag.rerank.model:BAAI/bge-reranker-v2-m3}")
    private String model;

    @Value("${llm.base-url:http://localhost:8000}")
    private String baseUrl;

    @Value("${llm.api-key:sk-placeholder}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Rerank retrieval results using cross-encoder.
     * Returns results sorted by rerank score if enabled, otherwise returns original.
     *
     * @param query original query
     * @param results retrieval results to rerank
     * @param topN number of top results to return after reranking
     * @return reranked results
     */
    public List<RetrievalResult> rerank(String query, List<RetrievalResult> results, int topN) {
        if (!enabled || results.isEmpty()) {
            log.debug("Rerank disabled or no results, returning original order");
            return results.stream().limit(topN).collect(Collectors.toList());
        }

        try {
            // Prepare documents for reranking
            List<Map<String, String>> documents = new ArrayList<>();
            for (int i = 0; i < results.size(); i++) {
                RetrievalResult r = results.get(i);
                documents.add(Map.of(
                    "text", r.getTitle() + "\n" + r.getContent(),
                    "id", String.valueOf(i)
                ));
            }

            // Build request
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("query", query);
            requestBody.put("documents", documents);
            requestBody.put("top_n", Math.min(topN, results.size()));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // Call rerank API
            String rerankUrl = baseUrl + "/v1/rerank";
            Map<String, Object> response = restTemplate.postForObject(rerankUrl, request, Map.class);

            if (response == null || !response.containsKey("results")) {
                log.warn("Rerank API returned invalid response, using original order");
                return results.stream().limit(topN).collect(Collectors.toList());
            }

            // Parse rerank results
            List<Map<String, Object>> rerankResults = (List<Map<String, Object>>) response.get("results");
            List<RetrievalResult> reranked = new ArrayList<>();

            for (Map<String, Object> rr : rerankResults) {
                int index = (int) rr.get("index");
                double score = (double) rr.get("relevance_score");

                RetrievalResult original = results.get(index);
                // Create new result with rerank score
                RetrievalResult rerankedResult = RetrievalResult.builder()
                    .documentId(original.getDocumentId())
                    .title(original.getTitle())
                    .content(original.getContent())
                    .score(score)
                    .source("rerank")
                    .build();
                reranked.add(rerankedResult);
            }

            log.info("Rerank completed: {} documents -> top {} results", results.size(), reranked.size());
            return reranked;

        } catch (Exception e) {
            log.error("Rerank failed, returning original order", e);
            return results.stream().limit(topN).collect(Collectors.toList());
        }
    }

    /**
     * Check if reranking is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }
}