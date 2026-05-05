package com.its.platform.api.controller;

import com.its.platform.common.result.Result;
import com.its.platform.core.session.SessionService;
import com.its.platform.infra.postgres.entity.SessionEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Session management API controller.
 */
@Slf4j
@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    /**
     * Create a new session.
     */
    @PostMapping
    public Result<SessionEntity> createSession() {
        SessionEntity session = sessionService.createSession();
        return Result.success(session);
    }

    /**
     * Get session by ID.
     */
    @GetMapping("/{sessionId}")
    public Result<SessionEntity> getSession(@PathVariable String sessionId) {
        SessionEntity session = sessionService.getSession(sessionId);
        if (session == null) {
            return Result.fail(404, "Session not found or expired");
        }
        return Result.success(session);
    }

    /**
     * Get all active sessions.
     */
    @GetMapping
    public Result<List<SessionEntity>> getActiveSessions() {
        List<SessionEntity> sessions = sessionService.getActiveSessions();
        return Result.success(sessions);
    }

    /**
     * Delete a session.
     */
    @DeleteMapping("/{sessionId}")
    public Result<Void> deleteSession(@PathVariable String sessionId) {
        sessionService.deleteSession(sessionId);
        return Result.success();
    }

    /**
     * Add a user message to session (for multi-turn conversation).
     */
    @PostMapping("/{sessionId}/messages")
    public Result<SessionEntity> addMessage(
            @PathVariable String sessionId,
            @RequestBody MessageRequest request) {
        SessionEntity session = sessionService.addMessage(sessionId, request.getRole(), request.getContent());
        if (session == null) {
            return Result.fail(404, "Session not found or expired");
        }
        return Result.success(session);
    }

    /**
     * Get session message history.
     */
    @GetMapping("/{sessionId}/messages")
    public Result<List<SessionEntity.MessageItem>> getMessages(@PathVariable String sessionId) {
        SessionEntity session = sessionService.getSession(sessionId);
        if (session == null) {
            return Result.fail(404, "Session not found or expired");
        }
        return Result.success(session.getMessages());
    }

    /**
     * Request body for adding a message.
     */
    @lombok.Data
    public static class MessageRequest {
        private String role;  // "user" or "assistant"
        private String content;
    }
}