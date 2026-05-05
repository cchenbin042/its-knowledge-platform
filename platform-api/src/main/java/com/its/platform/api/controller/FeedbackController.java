package com.its.platform.api.controller;

import com.its.platform.api.dto.FeedbackRequest;
import com.its.platform.common.result.Result;
import com.its.platform.core.feedback.FeedbackService;
import com.its.platform.core.feedback.FeedbackStats;
import com.its.platform.infra.postgres.entity.FeedbackEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Feedback API controller.
 */
@Slf4j
@RestController
@RequestMapping("/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    /**
     * Submit feedback (rating 1-5 scale).
     */
    @PostMapping
    public Result<FeedbackEntity> submitFeedback(@RequestBody FeedbackRequest request) {
        try {
            FeedbackEntity feedback = feedbackService.submitFeedback(
                request.getSessionId(),
                request.getQuestion(),
                request.getAnswer(),
                request.getRating(),
                request.getComment()
            );
            return Result.success(feedback);
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /**
     * Get feedback by ID.
     */
    @GetMapping("/{id}")
    public Result<FeedbackEntity> getFeedback(@PathVariable Long id) {
        FeedbackEntity feedback = feedbackService.getById(id);
        if (feedback == null) {
            return Result.fail(404, "Feedback not found");
        }
        return Result.success(feedback);
    }

    /**
     * Get feedback by session.
     */
    @GetMapping("/session/{sessionId}")
    public Result<List<FeedbackEntity>> getFeedbackBySession(@PathVariable String sessionId) {
        List<FeedbackEntity> feedbacks = feedbackService.getFeedbackBySession(sessionId);
        return Result.success(feedbacks);
    }

    /**
     * Get recent feedback.
     */
    @GetMapping("/recent")
    public Result<List<FeedbackEntity>> getRecentFeedback(@RequestParam(defaultValue = "7") int days) {
        List<FeedbackEntity> feedbacks = feedbackService.getRecentFeedback(days);
        return Result.success(feedbacks);
    }

    /**
     * Get feedback statistics.
     */
    @GetMapping("/stats")
    public Result<FeedbackStats> getStats() {
        FeedbackStats stats = feedbackService.getStats();
        return Result.success(stats);
    }

    /**
     * Delete feedback by ID.
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteFeedback(@PathVariable Long id) {
        feedbackService.deleteFeedback(id);
        return Result.success();
    }

    /**
     * Get all feedback.
     */
    @GetMapping
    public Result<List<FeedbackEntity>> getAllFeedback() {
        List<FeedbackEntity> feedbacks = feedbackService.getAllFeedback();
        return Result.success(feedbacks);
    }
}