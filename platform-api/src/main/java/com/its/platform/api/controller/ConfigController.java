package com.its.platform.api.controller;

import com.its.platform.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * System configuration API controller.
 */
@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
public class ConfigController {

    @Value("${llm.base-url:http://localhost:8000}")
    private String baseUrl;

    @Value("${llm.embedding-model:text-embedding-3-small}")
    private String embeddingModel;

    @Value("${llm.chat-model:gpt-4o-mini}")
    private String chatModel;

    /**
     * Get LLM configuration (non-sensitive info only).
     */
    @GetMapping("/llm")
    public Result<Map<String, String>> getLlmConfig() {
        return Result.success(Map.of(
            "baseUrl", baseUrl,
            "embeddingModel", embeddingModel,
            "chatModel", chatModel
        ));
    }
}