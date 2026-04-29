package com.its.platform.rag.llm;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
            .baseUrl(baseUrl)
            .apiKey(apiKey)
            .modelName(embeddingModel)
            .build();
    }

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
            .baseUrl(baseUrl)
            .apiKey(apiKey)
            .modelName(chatModel)
            .build();
    }
}