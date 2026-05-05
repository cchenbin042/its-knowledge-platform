package com.its.platform.core.feedback;

import lombok.Builder;
import lombok.Data;

/**
 * Feedback statistics.
 */
@Data
@Builder
public class FeedbackStats {
    private long totalCount;
    private double avgRating;        // Average rating (1-5 scale)
    private long positiveCount;      // Ratings >= 4
    private long negativeCount;      // Ratings <= 2
    private double satisfactionRate;  // Percentage of ratings >= 4
}