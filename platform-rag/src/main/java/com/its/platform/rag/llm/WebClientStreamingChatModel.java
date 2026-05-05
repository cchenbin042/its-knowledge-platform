package com.its.platform.rag.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于 WebClient 的 StreamingChatLanguageModel 实现
 */
@Slf4j
public class WebClientStreamingChatModel implements StreamingChatLanguageModel {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String modelName;

    public WebClientStreamingChatModel(WebClient webClient, ObjectMapper objectMapper, String modelName) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.modelName = modelName;
    }

    @Override
    public void generate(String userMessage, StreamingResponseHandler<AiMessage> handler) {
        generate(List.of(UserMessage.from(userMessage)), handler);
    }

    @Override
    public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
        try {
            // 转换消息格式
            List<Map<String, Object>> convertedMessages = new ArrayList<>();
            for (ChatMessage msg : messages) {
                String role = msg.type().name().toLowerCase();
                String content = extractContent(msg);
                convertedMessages.add(Map.of("role", role, "content", content));
            }

            Map<String, Object> requestBody = Map.of(
                "model", modelName,
                "messages", convertedMessages,
                "stream", true
            );

            log.debug("Starting streaming chat request to model: {}", modelName);

            Flux<String> stream = webClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class);

            StringBuilder fullContent = new StringBuilder();

            stream.doOnNext(line -> {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6);
                    if ("[DONE]".equals(data)) {
                        return;
                    }
                    try {
                        Map<String, Object> chunk = objectMapper.readValue(data, Map.class);
                        List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
                        if (choices != null && !choices.isEmpty()) {
                            Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                            if (delta != null && delta.containsKey("content")) {
                                String contentPiece = (String) delta.get("content");
                                fullContent.append(contentPiece);
                                if (handler != null) {
                                    handler.onNext(contentPiece);
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse SSE chunk: {}", data);
                    }
                }
            })
            .doOnComplete(() -> {
                if (handler != null) {
                    AiMessage aiMessage = AiMessage.from(fullContent.toString());
                    handler.onComplete(Response.from(aiMessage));
                }
                log.info("Streaming chat completed: {} chars", fullContent.length());
            })
            .doOnError(e -> {
                log.error("Streaming chat failed", e);
                if (handler != null) {
                    handler.onError(e);
                }
            })
            .subscribe();

        } catch (Exception e) {
            log.error("Streaming chat request failed", e);
            throw new RuntimeException("Streaming chat request failed", e);
        }
    }

    private String extractContent(ChatMessage msg) {
        if (msg instanceof UserMessage) {
            return ((UserMessage) msg).singleText();
        } else if (msg instanceof dev.langchain4j.data.message.SystemMessage) {
            return ((dev.langchain4j.data.message.SystemMessage) msg).text();
        } else if (msg instanceof AiMessage) {
            return ((AiMessage) msg).text();
        }
        return msg.toString();
    }
}