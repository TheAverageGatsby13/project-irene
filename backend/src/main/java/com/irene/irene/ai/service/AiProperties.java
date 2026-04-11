package com.irene.irene.ai.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
        String mode,
        String chatEndpoint,
        String embeddingEndpoint,
        String apiKey,
        String secondaryApiKey,
        String chatModel,
        String embeddingModel,
        String geminiApiKey,
        String geminiModel,
        String geminiEmbeddingModel,
        String geminiBaseUrl,
        Integer timeoutSeconds,
        Integer maxRetries,
        String compartmentId,
        String region
) {
}
