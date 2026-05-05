package com.its.platform.rag.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于 WebClient 的 ChatLanguageModel 实现
 */
@Slf4j
public class WebClientChatModel implements ChatLanguageModel {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String modelName;
    private final Duration timeout;

    public WebClientChatModel(WebClient webClient, ObjectMapper objectMapper, String modelName, Duration timeout) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.modelName = modelName;
        this.timeout = timeout;
    }

    @Override
    public Response<AiMessage> generate(ChatMessage... messages) {
        return generate(List.of(messages));
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
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
                "messages", convertedMessages
            );

            log.debug("Sending chat request to model: {}", modelName);

            String responseJson = webClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(timeout)
                .block();

            Map<String, Object> response = objectMapper.readValue(responseJson, Map.class);

            // 解析响应
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("No choices in response");
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");

            // 解析 token usage
            Map<String, Object> usage = (Map<String, Object>) response.get("usage");
            TokenUsage tokenUsage = null;
            if (usage != null) {
                int inputTokens = ((Number) usage.getOrDefault("prompt_tokens", 0)).intValue();
                int outputTokens = ((Number) usage.getOrDefault("completion_tokens", 0)).intValue();
                tokenUsage = new TokenUsage(inputTokens, outputTokens);
            }

            log.info("Chat response received: {} chars, {} tokens", content.length(), tokenUsage);

            AiMessage aiMessage = AiMessage.from(content);
            return Response.from(aiMessage, tokenUsage);

        } catch (Exception e) {
            log.error("Chat request failed: {}", e.getMessage(), e);
            throw new RuntimeException("Chat request failed", e);
        }
    }

    private String extractContent(ChatMessage msg) {
        if (msg instanceof dev.langchain4j.data.message.UserMessage) {
            return ((dev.langchain4j.data.message.UserMessage) msg).singleText();
        } else if (msg instanceof dev.langchain4j.data.message.SystemMessage) {
            return ((dev.langchain4j.data.message.SystemMessage) msg).text();
        } else if (msg instanceof dev.langchain4j.data.message.AiMessage) {
            return ((dev.langchain4j.data.message.AiMessage) msg).text();
        }
        return msg.toString();
    }
}