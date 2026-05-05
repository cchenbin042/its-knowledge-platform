package com.its.platform.api.controller;

import com.its.platform.common.result.Result;
import com.its.platform.core.analytics.QueryLogService;
import com.its.platform.core.analytics.QueryStats;
import com.its.platform.infra.postgres.entity.QueryLogEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Query analytics API controller.
 */
@Slf4j
@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final QueryLogService queryLogService;

    /**
     * Get query statistics.
     */
    @GetMapping
    public Result<QueryStats> getStats(@RequestParam(defaultValue = "7") int days) {
        QueryStats stats = queryLogService.getStats(days);
        return Result.success(stats);
    }

    /**
     * Get daily query counts.
     */
    @GetMapping("/daily")
    public Result<List<Map<String, Object>>> getDailyCounts(@RequestParam(defaultValue = "30") int days) {
        List<Map<String, Object>> counts = queryLogService.getDailyCounts(days);
        return Result.success(counts);
    }

    /**
     * Get top questions.
     */
    @GetMapping("/top-questions")
    public Result<List<Map<String, Object>>> getTopQuestions(
        @RequestParam(defaultValue = "7") int days,
        @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> questions = queryLogService.getTopQuestions(days, limit);
        return Result.success(questions);
    }

    /**
     * Get raw query logs.
     */
    @GetMapping("/logs")
    public Result<List<QueryLogEntity>> getLogs(@RequestParam(defaultValue = "7") int days) {
        List<QueryLogEntity> logs = queryLogService.getLogs(days);
        return Result.success(logs);
    }
}