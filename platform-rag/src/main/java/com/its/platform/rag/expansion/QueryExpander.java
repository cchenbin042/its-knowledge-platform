package com.its.platform.rag.expansion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Combined query expansion using both synonym and LLM methods.
 * Provides configurable expansion strategy.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueryExpander {

    private final SynonymExpander synonymExpander;
    private final LlmQueryExpander llmQueryExpander;

    /**
     * Expand query using all available methods.
     *
     * @param query original query
     * @return expanded and deduplicated queries
     */
    public List<String> expand(String query) {
        return expand(query, ExpansionStrategy.ALL);
    }

    /**
     * Expand query using specified strategy.
     *
     * @param query original query
     * @param strategy expansion strategy
     * @return expanded queries
     */
    public List<String> expand(String query, ExpansionStrategy strategy) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        Set<String> uniqueQueries = new HashSet<>();

        switch (strategy) {
            case SYNONYM_ONLY:
                uniqueQueries.addAll(synonymExpander.expand(query));
                break;

            case LLM_ONLY:
                uniqueQueries.addAll(llmQueryExpander.expand(query));
                break;

            case ALL:
                // Combine both methods
                List<String> synonymResults = synonymExpander.expand(query);
                uniqueQueries.addAll(synonymResults);

                // Expand the original query with LLM (not all synonym variants)
                List<String> llmResults = llmQueryExpander.expand(query);
                uniqueQueries.addAll(llmResults);

                break;

            case NONE:
                uniqueQueries.add(query);
                break;
        }

        // Ensure original query is always first
        List<String> result = new ArrayList<>();
        result.add(query);
        for (String q : uniqueQueries) {
            if (!q.equals(query)) {
                result.add(q);
            }
        }

        // Limit total expansions to avoid retrieval explosion
        if (result.size() > 5) {
            result = result.subList(0, 5);
        }

        log.info("Query expansion ({}): {} -> {} variants", strategy, query, result.size());
        return result;
    }

    public enum ExpansionStrategy {
        NONE,        // No expansion, return original only
        SYNONYM_ONLY, // Only synonym-based expansion
        LLM_ONLY,     // Only LLM-based expansion
        ALL           // Use all expansion methods
    }
}