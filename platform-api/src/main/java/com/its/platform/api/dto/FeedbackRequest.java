package com.its.platform.api.dto;

import lombok.Data;

/**
 * Feedback request DTO.
 */
@Data
public class FeedbackRequest {
    private String sessionId;
    private String question;
    private String answer;
    private Integer rating;    // 1-5 scale (1=very bad, 5=very good)
    private String comment;
}