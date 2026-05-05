package com.its.platform.rag.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
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
    private final Duration timeout;

    public WebClientStreamingChatModel(WebClient webClient, ObjectMapper objectMapper, String modelName, Duration timeout) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.modelName = modelName;
        this.timeout = timeout;
    }

    @Override
    public void generate(String userMessage, StreamingResponseHandler<AiMessage> handler) {
        generate(List.of(UserMessage.from(userMessage)), handler);
    }

    @Override
    public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
        Instant requestStart = Instant.now();

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

            // 请求前详细日志
            log.info("=== LLM REQUEST ===");
            log.info("URL: /chat/completions");
            log.info("Model: {}", modelName);
            log.info("Timeout: {}s", timeout.toSeconds());
            log.info("Messages count: {}", convertedMessages.size());
            log.info("Request body: {}", objectMapper.writeValueAsString(requestBody));
            log.info("Request start time: {}", requestStart);

            // 使用 DataBuffer 流手动解析 SSE，避免缓冲问题
            Flux<DataBuffer> dataBufferFlux = webClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .timeout(timeout);

            StringBuilder fullContent = new StringBuilder();
            StringBuilder lineBuffer = new StringBuilder();
            boolean[] firstChunkReceived = {false};

            // 直接处理每个 DataBuffer，不合并
            dataBufferFlux
                .doOnNext(buffer -> {
                    Instant chunkTime = Instant.now();
                    if (!firstChunkReceived[0]) {
                        firstChunkReceived[0] = true;
                        long latencyMs = java.time.Duration.between(requestStart, chunkTime).toMillis();
                        log.info("=== FIRST CHUNK RECEIVED ===");
                        log.info("Time to first chunk: {}ms", latencyMs);
                    }

                    // 将 DataBuffer 转换为字符串
                    byte[] bytes = new byte[buffer.readableByteCount()];
                    buffer.read(bytes);
                    DataBufferUtils.release(buffer);
                    String chunk = new String(bytes, StandardCharsets.UTF_8);

                    // 记录原始数据块
                    log.debug("Raw SSE chunk ({} bytes): {}", chunk.length(),
                        chunk.length() > 200 ? chunk.substring(0, 200) + "..." : chunk);

                    // 按行处理 SSE 数据
                    processSseLines(chunk, lineBuffer, fullContent, handler);
                })
                .doOnComplete(() -> {
                    long totalMs = java.time.Duration.between(requestStart, Instant.now()).toMillis();
                    log.info("=== LLM STREAM COMPLETE ===");
                    log.info("Total duration: {}ms", totalMs);
                    log.info("Total content length: {} chars", fullContent.length());

                    if (handler != null) {
                        AiMessage aiMessage = AiMessage.from(fullContent.toString());
                        handler.onComplete(Response.from(aiMessage));
                    }
                })
                .doOnError(e -> {
                    long elapsedMs = java.time.Duration.between(requestStart, Instant.now()).toMillis();
                    log.error("=== LLM STREAM ERROR ===");
                    log.error("Error after {}ms", elapsedMs);
                    log.error("Error type: {}", e.getClass().getName());
                    log.error("Error message: {}", e.getMessage());
                    if (e.getCause() != null) {
                        log.error("Cause: {} - {}", e.getCause().getClass().getName(), e.getCause().getMessage());
                    }

                    if (handler != null) {
                        handler.onError(e);
                    }
                })
                .subscribe();

        } catch (Exception e) {
            log.error("=== LLM REQUEST FAILED ===");
            log.error("Exception during request setup: {}", e.getMessage(), e);
            throw new RuntimeException("Streaming chat request failed", e);
        }
    }

    /**
     * 按行处理 SSE 数据
     */
    private void processSseLines(String chunk, StringBuilder lineBuffer,
                                  StringBuilder fullContent, StreamingResponseHandler<AiMessage> handler) {
        // 将新数据追加到行缓冲区
        lineBuffer.append(chunk);

        // 按换行符分割处理
        String bufferContent = lineBuffer.toString();
        int lastNewlineIndex = bufferContent.lastIndexOf('\n');

        if (lastNewlineIndex >= 0) {
            String completeLines = bufferContent.substring(0, lastNewlineIndex + 1);
            String remaining = bufferContent.substring(lastNewlineIndex + 1);

            // 重置缓冲区为剩余部分
            lineBuffer.setLength(0);
            lineBuffer.append(remaining);

            // 处理每一行
            for (String line : completeLines.split("\n")) {
                processSseLine(line.trim(), fullContent, handler);
            }
        }
    }

    /**
     * 处理单个 SSE 行
     */
    private void processSseLine(String line, StringBuilder fullContent, StreamingResponseHandler<AiMessage> handler) {
        if (line.isEmpty()) {
            return;
        }

        // 记录每一行（调试用）
        log.debug("SSE line: {}", line.length() > 100 ? line.substring(0, 100) + "..." : line);

        // SiliconFlow/OpenAI SSE 格式: "data: {...}" 或 "data: [DONE]"
        if (line.startsWith("data:")) {
            String data = line.substring(5).trim();

            if ("[DONE]".equals(data)) {
                log.debug("SSE stream marked as done");
                return;
            }

            if (data.isEmpty()) {
                return;
            }

            try {
                Map<String, Object> chunk = objectMapper.readValue(data, Map.class);
                List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");

                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, Object> delta = (Map<String, Object>) firstChoice.get("delta");

                    if (delta != null && delta.containsKey("content")) {
                        String contentPiece = (String) delta.get("content");
                        if (contentPiece != null && !contentPiece.isEmpty()) {
                            fullContent.append(contentPiece);
                            log.debug("Content piece: '{}'", contentPiece.length() > 50 ?
                                contentPiece.substring(0, 50) + "..." : contentPiece);

                            if (handler != null) {
                                handler.onNext(contentPiece);
                            }
                        }
                    }

                    // 检查 finish_reason
                    String finishReason = (String) firstChoice.get("finish_reason");
                    if (finishReason != null) {
                        log.debug("Finish reason: {}", finishReason);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse SSE data: '{}', error: {}", data.length() > 100 ?
                    data.substring(0, 100) + "..." : data, e.getMessage());
            }
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