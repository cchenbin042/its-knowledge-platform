package com.its.platform.rag.fusion;

import com.its.platform.rag.retriever.RetrievalResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Reciprocal Rank Fusion (RRF) algorithm for combining multiple retrieval results.
 *
 * RRF score formula: score(d) = sum(1 / (k + rank(d)) for each retrieval source)
 * where k is a constant (default 60) to reduce the impact of high rankings.
 *
 * Reference: https://plg.uwaterloo.ca/~gvcormac/cormacksigir09-rrf.pdf
 */
@Slf4j
@Component
public class RrfFusion {

    private final int k = 60;

    /**
     * Fuse multiple retrieval result lists using RRF algorithm.
     *
     * @param resultLists multiple retrieval result lists (e.g., vector + BM25)
     * @return fused and sorted results
     */
    public List<RetrievalResult> fuse(List<List<RetrievalResult>> resultLists) {
        if (resultLists == null || resultLists.isEmpty()) {
            return List.of();
        }

        // Single list - no fusion needed
        if (resultLists.size() == 1) {
            return resultLists.get(0);
        }

        // Compute RRF scores for each document
        Map<String, Double> rrfScores = new HashMap<>();
        Map<String, RetrievalResult> documentMap = new HashMap<>();

        for (List<RetrievalResult> results : resultLists) {
            for (int rank = 0; rank < results.size(); rank++) {
                RetrievalResult result = results.get(rank);
                String docId = result.getDocumentId();

                // Use documentId as key, but handle null
                String key = docId != null ? docId : result.getContent();

                // Accumulate RRF score
                double contribution = 1.0 / (k + rank + 1);
                rrfScores.merge(key, contribution, Double::sum);

                // Keep the best result instance (prefer higher score)
                if (!documentMap.containsKey(key) ||
                    result.getScore() > documentMap.get(key).getScore()) {
                    documentMap.put(key, result);
                }
            }
        }

        // Sort by RRF score descending
        List<RetrievalResult> fusedResults = documentMap.entrySet()
            .stream()
            .map(entry -> {
                RetrievalResult result = entry.getValue();
                // Update score to RRF score
                result.setScore(rrfScores.get(entry.getKey()));
                return result;
            })
            .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
            .collect(Collectors.toList());

        log.info("RRF fusion completed: {} documents from {} sources",
            fusedResults.size(), resultLists.size());

        return fusedResults;
    }

    /**
     * Fuse two retrieval result lists (common case: vector + BM25).
     */
    public List<RetrievalResult> fuse(List<RetrievalResult> vectorResults,
                                      List<RetrievalResult> bm25Results) {
        return fuse(List.of(vectorResults, bm25Results));
    }
}