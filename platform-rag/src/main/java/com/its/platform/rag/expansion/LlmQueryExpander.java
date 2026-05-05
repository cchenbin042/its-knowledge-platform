package com.its.platform.rag.expansion;

import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * LLM-based query expansion.
 * Uses LLM to generate alternative query formulations for better retrieval.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmQueryExpander {

    private final ChatLanguageModel chatModel;

    private static final String EXPANSION_PROMPT = """
        You are a query optimization expert for IT knowledge retrieval.
        Given the user's question, generate 2-3 alternative formulations that:
        1. Use different terminology but same intent
        2. Are more specific or more general versions
        3. Focus on key technical aspects

        Original question: %s

        Output ONLY the alternative queries, one per line, numbered:
        1. [first alternative]
        2. [second alternative]
        3. [third alternative]

        Keep each alternative concise (under 50 words).
        """;

    /**
     * Expand query using LLM.
     *
     * @param query original query
     * @return expanded queries including original
     */
    public List<String> expand(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        try {
            String prompt = String.format(EXPANSION_PROMPT, query);
            String response = chatModel.generate(prompt);

            List<String> expandedQueries = new ArrayList<>();
            expandedQueries.add(query);  // Always include original

            // Parse LLM response to extract alternative queries
            for (String line : response.split("\n")) {
                String cleaned = parseAlternativeQuery(line);
                if (cleaned != null && !cleaned.isEmpty() && !cleaned.equals(query)) {
                    expandedQueries.add(cleaned);
                }
            }

            log.info("LLM expansion: {} -> {} variants", query, expandedQueries.size());
            return expandedQueries;

        } catch (Exception e) {
            log.warn("LLM query expansion failed, returning original query", e);
            return List.of(query);
        }
    }

    private String parseAlternativeQuery(String line) {
        // Remove numbering prefix like "1. ", "2. ", etc.
        line = line.trim();
        if (line.isEmpty()) {
            return null;
        }

        // Match patterns like "1. query text" or "1) query text"
        if (line.matches("^\\d+[.)].+")) {
            return line.substring(line.indexOf('.') + 1).trim();
        }

        // If no numbering, return the line if it looks like a query
        if (line.length() > 5 && line.length() < 200) {
            return line;
        }

        return null;
    }
}