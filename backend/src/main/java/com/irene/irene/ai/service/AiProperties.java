package com.irene.irene.ai.service;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(String mode, String provider, String projectId, String location, String embeddingLocation,
        String geminiApiKey, String geminiModel, String geminiEmbeddingModel,
        String geminiBaseUrl, Integer timeoutSeconds) {}
