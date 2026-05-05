package com.its.platform.core.analytics;

import lombok.Builder;
import lombok.Data;

/**
 * Query statistics summary.
 */
@Data
@Builder
public class QueryStats {
    private long totalQueries;
    private double avgDurationMs;
    private double avgSourceCount;
    private double cacheHitRate;   // percentage
    private int periodDays;
}