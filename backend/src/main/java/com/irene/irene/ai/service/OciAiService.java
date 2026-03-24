package com.irene.irene.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.irene.irene.ai.model.ExtractedFeatures;
import com.irene.irene.ai.model.RiskLevel;
import com.irene.irene.common.util.VectorUtil;
import com.irene.irene.intake.model.IntakeSubmission;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@ConditionalOnProperty(prefix = "app.ai", name = "mode", havingValue = "oci")
public class OciAiService implements AiService {

    private static final int DEFAULT_EMBED_DIMENSIONS = 12;

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient client;

    public OciAiService(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds()))
                .build();
    }

    @Override
    public RiskLevel detectRisk(String intakeText) {
        String prompt = "Classify risk as LOW, MEDIUM, or HIGH for this therapy intake text. " +
                "Return only one token LOW/MEDIUM/HIGH. Text: " + intakeText;
        try {
            String content = chatCompletionWithFallback(prompt);
            String normalized = content.trim().toUpperCase(Locale.ROOT);
            if (normalized.contains("HIGH")) {
                return RiskLevel.HIGH;
            }
            if (normalized.contains("MEDIUM")) {
                return RiskLevel.MEDIUM;
            }
            return RiskLevel.LOW;
        } catch (Exception ex) {
            return fallbackRisk(intakeText);
        }
    }

    @Override
    public ExtractedFeatures extractFeatures(IntakeSubmission submission) {
        String prompt = "Extract strict JSON with fields issueTags(array), goals(array), summary(string), normalizedText(string). " +
                "Intake: " + submission.getFreeText() + " Goals: " + submission.getGoalsText();
        try {
            String content = chatCompletionWithFallback(prompt);
            String jsonPayload = extractJsonPayload(content);
            JsonNode node = objectMapper.readTree(jsonPayload);
            List<String> issueTags = toList(node.path("issueTags"));
            List<String> goals = toList(node.path("goals"));
            if (issueTags.isEmpty()) {
                issueTags = inferIssueTags(submission.getFreeText());
            }
            if (goals.isEmpty()) {
                goals = List.of("support");
            }
            String summary = node.path("summary").asText("Patient seeks support for " + String.join(", ", issueTags));
            String normalizedText = node.path("normalizedText")
                    .asText("issues=" + String.join("|", issueTags) + ";goals=" + String.join("|", goals));
            return new ExtractedFeatures(issueTags, goals, summary, normalizedText);
        } catch (Exception ex) {
            List<String> issueTags = inferIssueTags(submission.getFreeText());
            List<String> goals = inferGoals(submission.getGoalsText());
            return new ExtractedFeatures(
                    issueTags,
                    goals,
                    "Patient seeks support for " + String.join(", ", issueTags),
                    "issues=" + String.join("|", issueTags) + ";goals=" + String.join("|", goals)
            );
        }
    }

    @Override
    public float[] embed(String normalizedText) {
        try {
            return ociEmbedding(normalizedText);
        } catch (Exception ex) {
            try {
                if (hasGeminiFallback()) {
                    return geminiEmbedding(normalizedText);
                }
            } catch (Exception ignored) {
                // Fall through to deterministic embedding.
            }
            return VectorUtil.deterministicEmbedding(normalizedText, DEFAULT_EMBED_DIMENSIONS);
        }
    }

    private float[] ociEmbedding(String normalizedText) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "model", properties.embeddingModel(),
                "input", normalizedText
        ));
        JsonNode root = postOciJsonWithKeyFallback(properties.embeddingEndpoint(), body);
        JsonNode embeddingNode = root.path("data").path(0).path("embedding");
        if (!embeddingNode.isArray() || embeddingNode.isEmpty()) {
            throw new IllegalStateException("OCI embedding response missing vector values");
        }
        float[] vector = new float[embeddingNode.size()];
        for (int i = 0; i < embeddingNode.size(); i++) {
            vector[i] = (float) embeddingNode.get(i).asDouble();
        }
        return vector;
    }

    private String chatCompletionWithFallback(String prompt) throws Exception {
        try {
            return ociChatCompletion(prompt);
        } catch (Exception ociError) {
            if (!hasGeminiFallback()) {
                throw ociError;
            }
            return geminiChatCompletion(prompt);
        }
    }

    private String ociChatCompletion(String prompt) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "model", properties.chatModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a precise clinical intake assistant."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0
        ));
        JsonNode root = postOciJsonWithKeyFallback(properties.chatEndpoint(), body);
        String content = root.path("choices").path(0).path("message").path("content").asText();
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("OCI chat response missing content");
        }
        return content;
    }

    private JsonNode postOciJsonWithKeyFallback(String endpoint, String body) throws Exception {
        try {
            return postJsonWithRetries(endpoint, body, properties.apiKey(), true);
        } catch (Exception primaryError) {
            if (properties.secondaryApiKey() == null || properties.secondaryApiKey().isBlank()) {
                throw primaryError;
            }
            return postJsonWithRetries(endpoint, body, properties.secondaryApiKey(), true);
        }
    }

    private String geminiChatCompletion(String prompt) throws Exception {
        String endpoint = buildGeminiEndpoint(properties.geminiModel(), "generateContent");
        String combinedPrompt = "You are a precise clinical intake assistant. " +
                "Return compact responses, and when asked for JSON return valid JSON only.\n\nUser input: " + prompt;
        String body = objectMapper.writeValueAsString(Map.of(
                "contents", List.of(
                        Map.of("role", "user", "parts", List.of(Map.of("text", combinedPrompt)))
                )
        ));
        JsonNode root = postJsonWithRetries(endpoint, body, null, false);
        String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText();
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("Gemini chat response missing content");
        }
        return text;
    }

    private float[] geminiEmbedding(String normalizedText) throws Exception {
        String endpoint = buildGeminiEndpoint(properties.geminiEmbeddingModel(), "embedContent");
        String body = objectMapper.writeValueAsString(Map.of(
                "content", Map.of("parts", List.of(Map.of("text", normalizedText)))
        ));
        JsonNode root = postJsonWithRetries(endpoint, body, null, false);
        JsonNode embeddingNode = root.path("embedding").path("values");
        if (!embeddingNode.isArray() || embeddingNode.isEmpty()) {
            throw new IllegalStateException("Gemini embedding response missing values");
        }
        float[] vector = new float[embeddingNode.size()];
        for (int i = 0; i < embeddingNode.size(); i++) {
            vector[i] = (float) embeddingNode.get(i).asDouble();
        }
        return vector;
    }

    private JsonNode postJsonWithRetries(String endpoint, String body, String apiKey, boolean bearerAuth) throws Exception {
        int retries = Math.max(1, properties.maxRetries() == null ? 3 : properties.maxRetries());
        Exception lastException = null;

        for (int attempt = 1; attempt <= retries; attempt++) {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .timeout(Duration.ofSeconds(timeoutSeconds()))
                        .header("Content-Type", "application/json");

                if (bearerAuth) {
                    if (apiKey == null || apiKey.isBlank()) {
                        throw new IllegalStateException("OCI API key is missing");
                    }
                    builder.header("Authorization", "Bearer " + apiKey);
                }

                HttpRequest request = builder
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return objectMapper.readTree(response.body());
                }
                lastException = new RuntimeException("AI request failed with status " + response.statusCode());
            } catch (Exception ex) {
                lastException = ex;
            }

            try {
                Thread.sleep(200L * attempt);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                throw interruptedException;
            }
        }
        throw lastException;
    }

    private String buildGeminiEndpoint(String model, String action) {
        String selectedModel = (model == null || model.isBlank())
                ? ("generateContent".equals(action) ? "gemini-1.5-flash" : "text-embedding-004")
                : model;

        String base = properties.geminiBaseUrl();
        if (base == null || base.isBlank()) {
            base = "https://generativelanguage.googleapis.com/v1beta";
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        String key = properties.geminiApiKey();
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Gemini API key is missing");
        }
        String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8);

        return base + "/models/" + selectedModel + ":" + action + "?key=" + encodedKey;
    }

    private boolean hasGeminiFallback() {
        return properties.geminiApiKey() != null && !properties.geminiApiKey().isBlank();
    }

    private int timeoutSeconds() {
        return Math.max(5, properties.timeoutSeconds() == null ? 20 : properties.timeoutSeconds());
    }

    private String extractJsonPayload(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }

        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }
        return trimmed;
    }

    private static final List<String> HIGH_RISK_TERMS = List.of(
            "suicide",
            "kill myself",
            "end my life",
            "want to die",
            "hurt myself",
            "self harm",
            "cut myself",
            "no reason to live",
            "ending it all",
            "can't go on",
            "overdose"
    );

    private static final List<String> MEDIUM_RISK_TERMS = List.of(
            "panic",
            "panic attack",
            "severe anxiety",
            "depress",
            "hopeless",
            "trauma",
            "overwhelmed",
            "burnout",
            "extreme stress",
            "crying all the time"
    );

    private RiskLevel fallbackRisk(String text) {
        String lower = text == null ? "" : text.toLowerCase(Locale.ROOT);

        for (String term : HIGH_RISK_TERMS) {
            if (lower.contains(term)) {
                return RiskLevel.HIGH;
            }
        }

        for (String term : MEDIUM_RISK_TERMS) {
            if (lower.contains(term)) {
                return RiskLevel.MEDIUM;
            }
        }

        return RiskLevel.LOW;
    }

   /* private RiskLevel fallbackRisk(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("suicide") || lower.contains("kill myself") || lower.contains("self harm")) {
            return RiskLevel.HIGH;
        }
        if (lower.contains("panic") || lower.contains("severe")) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }*/

    /*private List<String> inferIssueTags(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        List<String> tags = new ArrayList<>();
        if (lower.contains("anx")) {
            tags.add("anxiety");
        }
        if (lower.contains("social")) {
            tags.add("social-anxiety");
        }
        if (lower.contains("lone")) {
            tags.add("loneliness");
        }
        if (lower.contains("grief")) {
            tags.add("grief");
        }
        if (lower.contains("depress") || lower.contains("hopeless") || lower.contains("empty")) {
            tags.add("depression");
        }
        if (lower.contains("panic")) {
            tags.add("panic-attacks");
        }
        if (lower.contains("confidence") || lower.contains("self esteem") || lower.contains("self-esteem")) {
            tags.add("low-self-esteem");
        }
        if (lower.contains("relationship") || lower.contains("partner") || lower.contains("marriage")) {
            tags.add("relationship-issues");
        }
        if (lower.contains("burnout") || lower.contains("work stress") || lower.contains("job")) {
            tags.add("burnout");
        }
        if (lower.contains("sleep") || lower.contains("insomnia")) {
            tags.add("sleep-issues");
        }
        if (lower.contains("trauma") || lower.contains("ptsd")) {
            tags.add("trauma");
        }
        if (lower.contains("anger") || lower.contains("angry")) {
            tags.add("anger");
        }
        if (lower.contains("alcohol") || lower.contains("drugs") || lower.contains("addiction")) {
            tags.add("substance-use");
        }
        if (lower.contains("family") || lower.contains("parents")) {
            tags.add("family-conflict");
        }
        if (tags.isEmpty()) {
            tags.add("stress");
        }
        return tags;
    }*/

    private List<String> inferIssueTags(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        List<String> tags = new ArrayList<>();

        if (lower.contains("anx")) tags.add("anxiety");
        if (lower.contains("panic")) tags.add("panic-attacks");
        if (lower.contains("depress")) tags.add("depression");
        if (lower.contains("social")) tags.add("social-anxiety");
        if (lower.contains("lone")) tags.add("loneliness");
        if (lower.contains("grief")) tags.add("grief");
        if (lower.contains("confidence") || lower.contains("self esteem")) tags.add("low-self-esteem");
        if (lower.contains("relationship")) tags.add("relationship-issues");
        if (lower.contains("burnout") || lower.contains("job")) tags.add("burnout");
        if (lower.contains("sleep") || lower.contains("insomnia")) tags.add("sleep-issues");
        if (lower.contains("trauma") || lower.contains("ptsd")) tags.add("trauma");
        if (lower.contains("anger")) tags.add("anger");
        if (lower.contains("alcohol") || lower.contains("drugs")) tags.add("substance-use");
        if (lower.contains("family")) tags.add("family-conflict");

        if (tags.isEmpty()) tags.add("stress");

        return tags;
    }

    private List<String> inferGoals(String goalsText) {
        List<String> list = goalsText == null ? List.of() : java.util.Arrays.stream(goalsText.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
        if (list.isEmpty()) {
            return List.of("support");
        }
        return list;
    }

    private List<String> toList(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<String> list = new ArrayList<>();
        for (JsonNode child : node) {
            if (child.asText() != null && !child.asText().isBlank()) {
                list.add(child.asText().trim());
            }
        }
        return list;
    }
}
