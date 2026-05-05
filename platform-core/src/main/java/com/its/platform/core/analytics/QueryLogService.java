package com.its.platform.core.analytics;

import com.its.platform.infra.postgres.entity.QueryLogEntity;
import com.its.platform.infra.postgres.repository.QueryLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Query log and analytics service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryLogService {

    private final QueryLogRepository queryLogRepository;

    /**
     * Log a query execution.
     *
     * @param sessionId session identifier
     * @param question user question
     * @param durationMs query duration in milliseconds
     * @param sourceCount number of retrieved sources
     * @param cacheHit whether result was cached
     * @param webSearchUsed whether web search was used
     */
    public void logQuery(String sessionId, String question, int durationMs,
                         int sourceCount, boolean cacheHit, boolean webSearchUsed) {
        QueryLogEntity queryLog = QueryLogEntity.builder()
            .sessionId(sessionId)
            .question(question)
            .durationMs(durationMs)
            .sourceCount(sourceCount)
            .cacheHit(cacheHit)
            .webSearchUsed(webSearchUsed)
            .createdAt(LocalDateTime.now())
            .build();

        queryLogRepository.save(queryLog);
        log.debug("Query logged: {}ms, {} sources", durationMs, sourceCount);
    }

    /**
     * Get query logs for a time period.
     */
    public List<QueryLogEntity> getLogs(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return queryLogRepository.findByCreatedAtAfter(since);
    }

    /**
     * Get daily query counts.
     */
    public List<Map<String, Object>> getDailyCounts(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return queryLogRepository.countByDate(since);
    }

    /**
     * Get aggregate statistics.
     */
    public QueryStats getStats(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        Map<String, Object> agg = queryLogRepository.aggregateStats(since);
        Long cacheHits = queryLogRepository.countCacheHits(since);
        Double avgDuration = queryLogRepository.averageDuration(since);

        long totalQueries = agg.get("total_queries") != null
            ? ((Number) agg.get("total_queries")).longValue() : 0;
        double avgSources = agg.get("avg_sources") != null
            ? ((Number) agg.get("avg_sources")).doubleValue() : 0;
        double avgDurationMs = avgDuration != null ? avgDuration : 0;
        double cacheHitRate = totalQueries > 0
            ? (double) cacheHits / totalQueries * 100 : 0;

        return QueryStats.builder()
            .totalQueries(totalQueries)
            .avgDurationMs(avgDurationMs)
            .avgSourceCount(avgSources)
            .cacheHitRate(cacheHitRate)
            .periodDays(days)
            .build();
    }

    /**
     * Get top questions.
     */
    public List<Map<String, Object>> getTopQuestions(int days, int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return queryLogRepository.topQuestions(since, limit);
    }
}