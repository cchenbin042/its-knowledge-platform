package com.its.platform.core.session;

import com.its.platform.infra.postgres.entity.SessionEntity;
import com.its.platform.infra.postgres.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Session management service.
 * Handles session creation, retrieval, renewal, and cleanup.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;

    private static final int SESSION_TIMEOUT_MINUTES = 30;

    /**
     * Create a new session.
     *
     * @return new session entity
     */
    public SessionEntity createSession() {
        String sessionId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(SESSION_TIMEOUT_MINUTES);

        SessionEntity session = SessionEntity.builder()
            .id(sessionId)
            .messages(List.of())
            .createdAt(now)
            .expiresAt(expiresAt)
            .build();

        sessionRepository.save(session);
        log.info("Session created: {}", sessionId);

        return session;
    }

    /**
     * Get session by ID, renewing if still valid.
     *
     * @param sessionId session identifier
     * @return session entity or null if expired/not found
     */
    public SessionEntity getSession(String sessionId) {
        Optional<SessionEntity> sessionOpt = sessionRepository.findById(sessionId);

        if (sessionOpt.isEmpty()) {
            log.warn("Session not found: {}", sessionId);
            return null;
        }

        SessionEntity session = sessionOpt.get();

        // Check expiration
        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Session expired: {}", sessionId);
            sessionRepository.deleteById(sessionId);
            return null;
        }

        // Renew session
        session.setExpiresAt(LocalDateTime.now().plusMinutes(SESSION_TIMEOUT_MINUTES));
        sessionRepository.save(session);

        return session;
    }

    /**
     * Add a message to session.
     *
     * @param sessionId session identifier
     * @param role message role (user/assistant)
     * @param content message content
     * @return updated session or null if invalid
     */
    public SessionEntity addMessage(String sessionId, String role, String content) {
        SessionEntity session = getSession(sessionId);
        if (session == null) {
            return null;
        }

        List<SessionEntity.MessageItem> messages = session.getMessages();
        SessionEntity.MessageItem newMessage = SessionEntity.MessageItem.builder()
            .role(role)
            .content(content)
            .timestamp(LocalDateTime.now())
            .build();

        messages.add(newMessage);
        session.setMessages(messages);

        sessionRepository.save(session);
        log.info("Message added to session {}: {} -> {}", sessionId, role, content.length());

        return session;
    }

    /**
     * Get all active sessions.
     */
    public List<SessionEntity> getActiveSessions() {
        return sessionRepository.findActiveSessions();
    }

    /**
     * Delete a session.
     */
    public void deleteSession(String sessionId) {
        sessionRepository.deleteById(sessionId);
        log.info("Session deleted: {}", sessionId);
    }

    /**
     * Scheduled cleanup of expired sessions (every hour).
     */
    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredSessions() {
        sessionRepository.deleteExpiredSessions();
        log.info("Expired sessions cleaned up");
    }
}