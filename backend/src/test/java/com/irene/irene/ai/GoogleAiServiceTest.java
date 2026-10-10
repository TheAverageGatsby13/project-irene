package com.irene.irene.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.irene.irene.ai.model.RiskLevel;
import com.irene.irene.ai.service.*;
import com.irene.irene.intake.model.IntakeSubmission;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class GoogleAiServiceTest {
    HttpServer server;
    GoogleAiService service;
    ObjectMapper mapper = new ObjectMapper();
    String response;
    int status;
    String observedKey;
    String observedQuery;
    @BeforeEach void setup() throws Exception {
        status = 200;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            observedKey = exchange.getRequestHeaders().getFirst("x-goog-api-key");
            observedQuery = exchange.getRequestURI().getQuery();
            exchange.getRequestBody().readAllBytes();
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        service = new GoogleAiService(new AiProperties("google", "gemini", "", "global", "us-central1", "synthetic-test-key",
                "test-model", "test-embedding", "http://127.0.0.1:" + server.getAddress().getPort(), 2), mapper);
    }
    @AfterEach void stop() { server.stop(0); }
    void candidate(String text) throws Exception {
        response = mapper.writeValueAsString(Map.of("candidates", List.of(Map.of("finishReason", "STOP", "content", Map.of("parts", List.of(Map.of("text", text)))))));
    }
    @Test void parsesStructuredRiskAndKeepsCredentialsOutOfUrl() throws Exception {
        candidate("{\"risk\":\"HIGH\"}");
        assertThat(service.detectRisk("fictional concern")).isEqualTo(RiskLevel.HIGH);
        assertThat(observedKey).isEqualTo("synthetic-test-key");
        assertThat(observedQuery).isNull();
    }
    @Test void malformedRiskNeedsReview() throws Exception {
        candidate("{\"risk\":\"probably safe\"}");
        assertThat(service.detectRisk("fictional concern")).isEqualTo(RiskLevel.REVIEW_REQUIRED);
    }
    @Test void outageNeedsReview() {
        status = 429; response = "quota exceeded";
        assertThat(service.detectRisk("fictional concern")).isEqualTo(RiskLevel.REVIEW_REQUIRED);
    }
    @Test void failedEmbeddingNeverReturnsFakeVector() {
        status = 503; response = "unavailable";
        assertThatThrownBy(() -> service.embed("fictional goals")).isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(503));
    }
    @Test void rejectsWrongDimension() {
        response = "{\"embedding\":{\"values\":[0.1,0.2]}}";
        assertThatThrownBy(() -> service.embed("fictional goals")).isInstanceOf(ResponseStatusException.class);
    }
    @Test void parsesSummaryAndRejectsInvalidFields() throws Exception {
        IntakeSubmission intake = new IntakeSubmission();
        intake.setFreeText("Fictional stress"); intake.setGoalsText("routine"); intake.setStructuredAnswers("{}");
        candidate("{\"issueTags\":[\"stress\"],\"goals\":[\"routine\"],\"summary\":\"Seeking support\",\"normalizedText\":\"stress routine\"}");
        assertThat(service.extractFeatures(intake).summary()).isEqualTo("Seeking support");
        candidate("{}");
        assertThatThrownBy(() -> service.extractFeatures(intake)).isInstanceOf(ResponseStatusException.class);
    }
}
