package com.its.platform.api.controller;

import com.its.platform.common.result.Result;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check API controller.
 * Provides service status for PostgreSQL, Redis, and LLM.
 */
@Slf4j
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;
    private final StringRedisTemplate redisTemplate;
    private final ChatLanguageModel chatModel;

    /**
     * Get overall health status.
     */
    @GetMapping
    public Result<Map<String, Object>> health() {
        Map<String, Object> status = new LinkedHashMap<>();

        status.put("status", "UP");
        status.put("timestamp", System.currentTimeMillis());

        // Check PostgreSQL
        status.put("postgres", checkPostgres());

        // Check Redis
        status.put("redis", checkRedis());

        // Check LLM
        status.put("llm", checkLlm());

        // Determine overall status
        boolean allHealthy = status.values().stream()
            .filter(v -> v instanceof Map)
            .allMatch(v -> "UP".equals(((Map<?, ?>) v).get("status")));

        if (!allHealthy) {
            status.put("status", "DEGRADED");
        }

        return Result.success(status);
    }

    /**
     * Check PostgreSQL connectivity.
     */
    private Map<String, Object> checkPostgres() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            Connection conn = dataSource.getConnection();
            boolean valid = conn.isValid(5);
            conn.close();

            result.put("status", valid ? "UP" : "DOWN");
            result.put("message", valid ? "Connection successful" : "Connection invalid");
        } catch (Exception e) {
            result.put("status", "DOWN");
            result.put("message", e.getMessage());
            log.warn("PostgreSQL health check failed", e);
        }
        return result;
    }

    /**
     * Check Redis connectivity.
     */
    private Map<String, Object> checkRedis() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            String response = redisTemplate.getConnectionFactory()
                .getConnection()
                .ping();

            result.put("status", "PONG".equals(response) ? "UP" : "DOWN");
            result.put("message", response);
        } catch (Exception e) {
            result.put("status", "DOWN");
            result.put("message", e.getMessage());
            log.warn("Redis health check failed", e);
        }
        return result;
    }

    /**
     * Check LLM service availability.
     * Note: This is a lightweight check - actual model calls happen during queries.
     */
    private Map<String, Object> checkLlm() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            // Simple ping - check if model is configured
            if (chatModel != null) {
                result.put("status", "UP");
                result.put("message", "LLM model configured");
            } else {
                result.put("status", "DOWN");
                result.put("message", "LLM model not configured");
            }
        } catch (Exception e) {
            result.put("status", "DOWN");
            result.put("message", e.getMessage());
            log.warn("LLM health check failed", e);
        }
        return result;
    }

    /**
     * Quick liveness check.
     */
    @GetMapping("/live")
    public Result<Map<String, Object>> liveness() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "UP");
        status.put("timestamp", System.currentTimeMillis());
        return Result.success(status);
    }

    /**
     * Readiness check (all dependencies healthy).
     */
    @GetMapping("/ready")
    public Result<Map<String, Object>> readiness() {
        Map<String, Object> status = new LinkedHashMap<>();

        boolean postgresReady = "UP".equals(checkPostgres().get("status"));
        boolean redisReady = "UP".equals(checkRedis().get("status"));

        status.put("status", postgresReady && redisReady ? "UP" : "DOWN");
        status.put("postgresReady", postgresReady);
        status.put("redisReady", redisReady);
        status.put("timestamp", System.currentTimeMillis());

        return Result.success(status);
    }
}