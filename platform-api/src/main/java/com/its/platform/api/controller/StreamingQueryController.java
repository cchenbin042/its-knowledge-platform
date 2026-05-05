package com.its.platform.api.controller;

import com.its.platform.api.dto.QueryRequest;
import com.its.platform.core.query.SseContext;
import com.its.platform.core.query.StreamingQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * SSE streaming query controller.
 * Provides real-time progress updates during query execution.
 */
@Slf4j
@RestController
@RequestMapping("/query")
@RequiredArgsConstructor
public class StreamingQueryController {

    private final StreamingQueryService streamingQueryService;
    private final SseContext sseContext;

    /**
     * Streaming query endpoint.
     * Returns SSE events for retrieval progress and LLM token stream.
     *
     * @param request query request containing the question and optional sessionId
     * @return SSE emitter that streams events
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter queryStream(@RequestBody QueryRequest request) {
        // Create SSE emitter with 5 minute timeout
        SseEmitter emitter = new SseEmitter(300000L);

        String sseSessionId = UUID.randomUUID().toString();
        sseContext.register(sseSessionId, emitter);

        log.info("Starting streaming query: sseSessionId={}, question={}, conversationSession={}",
            sseSessionId, request.getQuestion(), request.getSessionId());

        // Execute streaming query asynchronously
        new Thread(() -> {
            try {
                streamingQueryService.queryStream(
                    request.getQuestion(),
                    sseSessionId,
                    request.getSessionId()  // Conversation session for multi-turn
                );
            } catch (Exception e) {
                log.error("Streaming query error", e);
                sseContext.completeWithError(sseSessionId, e);
            }
        }, "sse-query-" + sseSessionId).start();

        return emitter;
    }
}