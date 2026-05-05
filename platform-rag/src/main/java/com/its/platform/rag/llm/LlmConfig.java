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

    @Bean
    public WebClient llmWebClient(ObjectMapper objectMapper) {
        log.info("Creating LLM WebClient: baseUrl={}, timeout={}s", baseUrl, timeoutSeconds);

        HttpClient httpClient = HttpClient.create()
            .responseTimeout(Duration.ofSeconds(timeoutSeconds));

        return WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .defaultHeader("Content-Type", "application/json")
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        log.info("Creating EmbeddingModel: model={}", embeddingModel);
        // Embedding 请求较快，可继续使用 LangChain4j 默认实现
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
        log.info("Creating StreamingChatLanguageModel with WebClient: model={}", chatModel);
        return new WebClientStreamingChatModel(llmWebClient, objectMapper, chatModel);
    }
}