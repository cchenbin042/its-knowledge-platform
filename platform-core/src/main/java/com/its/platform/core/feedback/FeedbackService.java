package com.its.platform.core.feedback;

import com.its.platform.infra.postgres.entity.FeedbackEntity;
import com.its.platform.infra.postgres.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Feedback service.
 * Handles user feedback submission and statistics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    /**
     * Submit user feedback.
     *
     * @param sessionId session identifier
     * @param question the original question
     * @param answer the AI answer
     * @param rating user rating (1-5 scale)
     * @param comment optional comment
     * @return saved feedback entity
     */
    public FeedbackEntity submitFeedback(String sessionId, String question, String answer,
                                          Integer rating, String comment) {
        // Validate rating range
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        FeedbackEntity feedback = FeedbackEntity.builder()
            .sessionId(sessionId)
            .question(question)
            .answer(answer)
            .rating(rating)
            .comment(comment)
            .createdAt(LocalDateTime.now())
            .build();

        feedbackRepository.save(feedback);
        log.info("Feedback submitted: session={}, rating={}", sessionId, rating);

        return feedback;
    }

    /**
     * Get feedback by ID.
     */
    public FeedbackEntity getById(Long id) {
        return feedbackRepository.findById(id);
    }

    /**
     * Get feedback by session ID.
     */
    public List<FeedbackEntity> getFeedbackBySession(String sessionId) {
        return feedbackRepository.findBySessionId(sessionId);
    }

    /**
     * Get recent feedback (last N days).
     */
    public List<FeedbackEntity> getRecentFeedback(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return feedbackRepository.findByCreatedAtAfter(since);
    }

    /**
     * Get feedback statistics.
     */
    public FeedbackStats getStats() {
        List<FeedbackEntity> allFeedback = feedbackRepository.findAll();

        long totalCount = allFeedback.size();
        long positiveCount = allFeedback.stream().filter(f -> f.getRating() >= 4).count();
        long negativeCount = allFeedback.stream().filter(f -> f.getRating() <= 2).count();

        double avgRating = totalCount > 0
            ? allFeedback.stream().mapToInt(FeedbackEntity::getRating).average().orElse(0.0)
            : 0.0;

        double satisfactionRate = totalCount > 0
            ? (double) positiveCount / totalCount * 100
            : 0.0;

        return FeedbackStats.builder()
            .totalCount(totalCount)
            .avgRating(avgRating)
            .positiveCount(positiveCount)
            .negativeCount(negativeCount)
            .satisfactionRate(satisfactionRate)
            .build();
    }

    /**
     * Delete feedback by ID.
     */
    public void deleteFeedback(Long id) {
        feedbackRepository.deleteById(id);
        log.info("Feedback deleted: {}", id);
    }

    /**
     * Get all feedback.
     */
    public List<FeedbackEntity> getAllFeedback() {
        return feedbackRepository.findAll();
    }
}