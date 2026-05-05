package com.its.platform.rag.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

/**
 * LLM 配置，使用 Spring WebClient 替代 LangChain4j 默认的 OkHttp
 */
@Slf4j
@Configuration
public class LlmConfig {

    @Value("${llm.base-url:http://localhost:8000}")
    private String baseUrl;

    @Value("${llm.api-key:sk-placeholder}")
    private String apiKey;

    @Value("${llm.embedding-model:text-embedding-3-small}")
    private String embeddingModel;

    @Value("${llm.chat-model:gpt-4o-mini}")
    private String chatModel;

    @Value("${llm.timeout-seconds:120}")
    private int timeoutSeconds;

    @Value("${llm.connect-timeout-seconds:30}")
    private int connectTimeoutSeconds;

    @Bean
    public WebClient llmWebClient(ObjectMapper objectMapper) {
        log.info("=== LLM WebClient Configuration ===");
        log.info("baseUrl: {}", baseUrl);
        log.info("apiKey: {} (length: {})", apiKey.substring(0, Math.min(10, apiKey.length())) + "...", apiKey.length());
        log.info("timeout: {}s", timeoutSeconds);
        log.info("connectTimeout: {}s", connectTimeoutSeconds);

        // 配置连接池
        ConnectionProvider connectionProvider = ConnectionProvider.builder("llm-pool")
            .maxConnections(10)
            .pendingAcquireTimeout(Duration.ofSeconds(60))
            .pendingAcquireMaxCount(-1)
            .maxIdleTime(Duration.ofSeconds(120))
            .maxLifeTime(Duration.ofSeconds(300))
            .build();

        // 配置 HttpClient：连接超时 + 响应超时 + 日志
        HttpClient httpClient = HttpClient.create(connectionProvider)
            .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutSeconds * 1000)
            .responseTimeout(Duration.ofSeconds(timeoutSeconds))
            .doOnConnected(conn -> {
                log.debug("HTTP connection established: {}", conn.channel());
                conn.addHandlerLast(
                    new io.netty.handler.timeout.ReadTimeoutHandler(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS));
            })
            .doOnRequest((req, conn) -> {
                log.debug("=== HTTP Request ===");
                log.debug("Method: {}", req.method());
                log.debug("URI: {}", req.uri());
                log.debug("Headers: {}", req.requestHeaders());
            })
            .doOnResponse((res, conn) -> {
                log.debug("=== HTTP Response ===");
                log.debug("Status: {}", res.status());
                log.debug("Headers: {}", res.responseHeaders());
            });

        WebClient webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("Accept", "text/event-stream")  // 显式添加 Accept header
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();

        log.info("LLM WebClient created successfully");
        return webClient;
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        log.info("Creating EmbeddingModel: model={}", embeddingModel);
        return OpenAiEmbeddingModel.builder()
            .baseUrl(baseUrl)
            .apiKey(apiKey)
            .modelName(embeddingModel)
            .timeout(Duration.ofSeconds(60))
            .build();
    }

    @Bean
    public ChatLanguageModel chatLanguageModel(WebClient llmWebClient, ObjectMapper objectMapper) {
        log.info("Creating ChatLanguageModel with WebClient: model={}, timeout={}s", chatModel, timeoutSeconds);
        return new WebClientChatModel(llmWebClient, objectMapper, chatModel, Duration.ofSeconds(timeoutSeconds));
    }

    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel(WebClient llmWebClient, ObjectMapper objectMapper) {
        log.info("Creating StreamingChatLanguageModel with WebClient: model={}, timeout={}s", chatModel, timeoutSeconds);
        return new WebClientStreamingChatModel(llmWebClient, objectMapper, chatModel, Duration.ofSeconds(timeoutSeconds));
    }
}