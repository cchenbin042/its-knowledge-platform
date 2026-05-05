package com.its.platform.core.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Query cache service using Redis.
 * Caches popular query answers with configurable TTL.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_KEY_PREFIX = "query:cache:";
    private static final int DEFAULT_TTL_SECONDS = 300;  // 5 minutes

    /**
     * Get cached response for a question.
     *
     * @param question the user question
     * @return cached QueryResponse or null if not cached
     */
    public QueryResponse get(String question) {
        String key = buildKey(question);
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                QueryResponse response = objectMapper.readValue(cached, QueryResponse.class);
                response.setCached(true);  // Mark as cached
                log.debug("Cache hit for question: {}", question);
                return response;
            }
        } catch (Exception e) {
            log.warn("Failed to read cache for question: {}", question, e);
        }
        return null;
    }

    /**
     * Cache a query response.
     *
     * @param question the user question
     * @param response the query response to cache
     */
    public void put(String question, QueryResponse response) {
        String key = buildKey(question);
        try {
            // Don't cache if already marked as cached
            if (response.isCached()) {
                return;
            }

            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, DEFAULT_TTL_SECONDS, TimeUnit.SECONDS);
            log.debug("Cached response for question: {}", question);
        } catch (Exception e) {
            log.warn("Failed to cache response for question: {}", question, e);
        }
    }

    /**
     * Check if a question is cached.
     */
    public boolean isCached(String question) {
        String key = buildKey(question);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Clear cache for a specific question.
     */
    public void evict(String question) {
        String key = buildKey(question);
        redisTemplate.delete(key);
        log.debug("Evicted cache for question: {}", question);
    }

    /**
     * Clear all query caches.
     */
    public void evictAll() {
        var keys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Evicted all query caches: {} entries", keys.size());
        }
    }

    private String buildKey(String question) {
        // Normalize question for consistent caching
        String normalized = question.toLowerCase().trim();
        return CACHE_KEY_PREFIX + normalized.hashCode();
    }
}