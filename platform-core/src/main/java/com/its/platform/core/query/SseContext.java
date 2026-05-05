package com.its.platform.core.query;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE context for managing multiple SseEmitter instances.
 * Allows different parts of the query pipeline to send events.
 */
@Slf4j
@Component
public class SseContext {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public void register(String sessionId, SseEmitter emitter) {
        emitters.put(sessionId, emitter);
        emitter.onCompletion(() -> emitters.remove(sessionId));
        emitter.onTimeout(() -> {
            log.warn("SSE timeout for session: {}", sessionId);
            emitters.remove(sessionId);
        });
        emitter.onError(e -> {
            log.error("SSE error for session: {}", sessionId, e);
            emitters.remove(sessionId);
        });
    }

    public void sendEvent(String sessionId, String eventType, Object data) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter == null) {
            log.warn("No emitter for session: {}", sessionId);
            return;
        }

        try {
            SseEmitter.SseEventBuilder event = SseEmitter.event()
                .name(eventType)
                .data(data);

            emitter.send(event);
            log.debug("SSE event sent: {} -> {}", sessionId, eventType);
        } catch (IOException e) {
            log.error("Failed to send SSE event: {}", eventType, e);
            emitters.remove(sessionId);
        }
    }

    public void complete(String sessionId) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter != null) {
            emitter.complete();
            emitters.remove(sessionId);
        }
    }

    public void completeWithError(String sessionId, Exception e) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter != null) {
            emitter.completeWithError(e);
            emitters.remove(sessionId);
        }
    }

    public boolean hasEmitter(String sessionId) {
        return emitters.containsKey(sessionId);
    }
}