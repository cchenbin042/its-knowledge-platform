package com.its.platform.api.dto;

import lombok.Data;

@Data
public class QueryRequest {
    private String question;
    private String sessionId;  // Optional: for multi-turn conversation
    private boolean expandQuery = false;  // Enable query expansion
    private boolean rerank = false;  // Enable reranking
    private int topN = 8;  // Number of results to return
}