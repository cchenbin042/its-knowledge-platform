package com.its.platform.core.query;

import lombok.Builder;
import lombok.Data;

/**
 * Query execution options.
 */
@Data
@Builder
public class QueryOptions {
    private boolean expandQuery;
    private boolean rerank;
    private int topN;

    public static QueryOptions defaultOptions() {
        return QueryOptions.builder()
            .expandQuery(false)   // Expansion disabled by default
            .rerank(false)        // Rerank disabled by default (config-controlled)
            .topN(8)
            .build();
    }

    public static QueryOptions withExpansion() {
        return QueryOptions.builder()
            .expandQuery(true)
            .rerank(false)
            .topN(8)
            .build();
    }

    public static QueryOptions withRerank() {
        return QueryOptions.builder()
            .expandQuery(false)
            .rerank(true)
            .topN(8)
            .build();
    }

    public static QueryOptions fullPipeline() {
        return QueryOptions.builder()
            .expandQuery(true)
            .rerank(true)
            .topN(8)
            .build();
    }
}