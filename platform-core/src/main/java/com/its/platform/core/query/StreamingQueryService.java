package com.its.platform.core.query;

import com.its.platform.core.analytics.QueryLogService;
import com.its.platform.core.session.SessionService;
import com.its.platform.infra.postgres.entity.SessionEntity;
import com.its.platform.rag.retriever.CompositeRetriever;
import com.its.platform.rag.retriever.RetrievalResult;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Streaming query service that pushes progress events via SSE.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingQueryService {

    private final CompositeRetriever compositeRetriever;
    private final StreamingChatLanguageModel streamingChatModel;
    private final SseContext sseContext;

    @Autowired(required = false)
    private QueryLogService queryLogService;

    @Autowired(required = false)
    private SessionService sessionService;

    /**
     * Execute streaming query and send progress events.
     *
     * @param question the user question
     * @param sessionId SSE session identifier
     */
    public void queryStream(String question, String sessionId) {
        queryStream(question, sessionId, null);
    }

    /**
     * Execute streaming query with conversation session support.
     *
     * @param question the user question
     * @param sseSessionId SSE session identifier for event streaming
     * @param conversationSessionId optional conversation session ID for multi-turn
     */
    public void queryStream(String question, String sseSessionId, String conversationSessionId) {
        long startTime = System.currentTimeMillis();

        try {
            // Step 1: Send query start event
            sseContext.sendEvent(sseSessionId, "query_start",
                Map.of("message", "Query started", "question", question,
                       "hasSession", conversationSessionId != null));

            // Step 2: Get conversation history if available
            List<SessionEntity.MessageItem> history = null;
            if (conversationSessionId != null && sessionService != null) {
                SessionEntity session = sessionService.getSession(conversationSessionId);
                if (session != null) {
                    history = session.getMessages();
                    sseContext.sendEvent(sseSessionId, "session_loaded",
                        Map.of("message", "Session loaded", "historyCount", history.size()));
                }
            }

            // Step 3: Send retrieval start event
            sseContext.sendEvent(sseSessionId, "retrieval_start",
                Map.of("message", "Starting retrieval", "question", question));

            // Step 4: Retrieve with RRF fusion
            List<RetrievalResult> results = compositeRetriever.retrieve(question);

            // Step 5: Send retrieval results event
            sseContext.sendEvent(sseSessionId, "retrieval_complete",
                Map.of("count", results.size(), "message", "Retrieval completed"));

            // Step 6: Take top N for context
            List<RetrievalResult> topResults = results.stream()
                .limit(8)
                .collect(Collectors.toList());

            // Step 7: Build context with history
            String context = buildContext(topResults, history);
            String prompt = buildPrompt(question, context, history);

            // Step 8: Send context built event
            sseContext.sendEvent(sseSessionId, "context_built",
                Map.of("sources", topResults.size(), "contextLength", context.length(),
                       "hasHistory", history != null && !history.isEmpty()));

            // Step 9: Send LLM start event
            sseContext.sendEvent(sseSessionId, "llm_start",
                Map.of("message", "LLM generation started"));

            // Step 10: Stream LLM response
            StringBuilder fullAnswer = new StringBuilder();

            streamingChatModel.generate(prompt, new StreamingResponseHandler<AiMessage>() {
                @Override
                public void onNext(String token) {
                    fullAnswer.append(token);
                    sseContext.sendEvent(sseSessionId, "llm_token", token);
                }

                @Override
                public void onComplete(Response<AiMessage> response) {
                    int durationMs = (int) (System.currentTimeMillis() - startTime);

                    // Save to conversation history
                    if (conversationSessionId != null && sessionService != null) {
                        sessionService.addMessage(conversationSessionId, "user", question);
                        sessionService.addMessage(conversationSessionId, "assistant", fullAnswer.toString());
                    }

                    // Log query execution
                    if (queryLogService != null) {
                        queryLogService.logQuery(conversationSessionId, question, durationMs,
                            topResults.size(), false, false);
                    }

                    // Send sources with final event
                    Map<String, Object> completeData = new HashMap<>();
                    completeData.put("answer", fullAnswer.toString());
                    completeData.put("sources", topResults);
                    completeData.put("durationMs", durationMs);
                    if (conversationSessionId != null) {
                        completeData.put("sessionId", conversationSessionId);
                    }
                    sseContext.sendEvent(sseSessionId, "llm_complete",
                        Map.of("message", "LLM generation completed", "tokenCount", fullAnswer.length()));

                    sseContext.sendEvent(sseSessionId, "complete", completeData);
                    sseContext.complete(sseSessionId);

                    log.info("Streaming query completed in {}ms: {} tokens, {} sources",
                        durationMs, fullAnswer.length(), topResults.size());
                }

                @Override
                public void onError(Throwable error) {
                    log.error("Streaming LLM error", error);
                    String errorMsg = error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
                    sseContext.sendEvent(sseSessionId, "llm_error",
                        Map.of("message", errorMsg));
                    sseContext.sendEvent(sseSessionId, "error",
                        Map.of("message", errorMsg));
                    sseContext.completeWithError(sseSessionId, new RuntimeException(error));
                }
            });

        } catch (Exception e) {
            log.error("Streaming query failed", e);
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            sseContext.sendEvent(sseSessionId, "error",
                Map.of("message", errorMsg));
            sseContext.completeWithError(sseSessionId, e);
        }
    }

    private String buildContext(List<RetrievalResult> results, List<SessionEntity.MessageItem> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("Reference documents:\n\n");
        for (int i = 0; i < results.size(); i++) {
            RetrievalResult r = results.get(i);
            sb.append(String.format("[%d] %s\n%s\n\n", i + 1, r.getTitle(), r.getContent()));
        }

        // Add history if available
        if (history != null && !history.isEmpty()) {
            sb.append("\n=== Conversation History ===\n");
            for (SessionEntity.MessageItem msg : history) {
                sb.append(String.format("%s: %s\n", msg.getRole().toUpperCase(), msg.getContent()));
            }
        }

        return sb.toString();
    }

    private String buildPrompt(String question, String context, List<SessionEntity.MessageItem> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an IT operations expert. Answer the user's question based on the following reference documents.\n\n");
        sb.append(context);
        sb.append(String.format("\nUser question: %s\n\n", question));
        sb.append("Please provide an accurate, detailed answer based on the reference documents. ");
        if (history != null && !history.isEmpty()) {
            sb.append("Consider the conversation history when answering. ");
        }
        sb.append("If there is no relevant information, please state that.");

        return sb.toString();
    }
}