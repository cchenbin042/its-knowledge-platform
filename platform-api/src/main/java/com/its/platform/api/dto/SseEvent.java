package com.its.platform.api.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * SSE event for streaming query progress.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SseEvent {
    private String eventType;
    private String message;
    private Object data;

    public static final String EVENT_RETRIEVAL_START = "retrieval_start";
    public static final String EVENT_RETRIEVAL_VECTOR = "retrieval_vector";
    public static final String EVENT_RETRIEVAL_BM25 = "retrieval_bm25";
    public static final String EVENT_RETRIEVAL_FUSED = "retrieval_fused";
    public static final String EVENT_CONTEXT_BUILT = "context_built";
    public static final String EVENT_LLM_TOKEN = "llm_token";
    public static final String EVENT_LLM_DONE = "llm_done";
    public static final String EVENT_ERROR = "error";
    public static final String EVENT_COMPLETE = "complete";
}