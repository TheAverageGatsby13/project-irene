package com.irene.irene.intake;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.irene.irene.ai.model.ExtractedFeatures;
import com.irene.irene.ai.model.RiskLevel;
import com.irene.irene.ai.service.AiService;
import com.irene.irene.auth.model.Role;
import com.irene.irene.auth.model.User;
import com.irene.irene.auth.repository.UserRepository;
import com.irene.irene.intake.repository.IntakeSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IntakeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private IntakeSubmissionRepository intakeSubmissionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiService aiService;

    @BeforeEach
    void setup() {
        intakeSubmissionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void intakeShouldPersistAndInvokeAiPipeline() throws Exception {
        User patient = new User();
        patient.setEmail("intake_patient@irene.app");
        patient.setPasswordHash(passwordEncoder.encode("Password@123"));
        patient.setRole(Role.PATIENT);
        userRepository.save(patient);

        when(aiService.detectRisk(anyString())).thenReturn(RiskLevel.MEDIUM);
        when(aiService.extractFeatures(any())).thenReturn(new ExtractedFeatures(
                java.util.List.of("anxiety", "loneliness"),
                java.util.List.of("confidence", "support"),
                "summary",
                "normalized"
        ));
        when(aiService.embed(anyString())).thenReturn(new float[]{0.2f, 0.4f, 0.8f, 0.1f, 0.3f, 0.6f, 0.9f, 0.7f, 0.2f, 0.5f, 0.1f, 0.4f});

        String loginBody = """
                {
                  "email": "intake_patient@irene.app",
                  "password": "Password@123"
                }
                """;

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode loginNode = objectMapper.readTree(loginResponse);
        String token = loginNode.get("token").asText();

        String intakeBody = """
                {
                  "ageBand":"18-24",
                  "languageCode":"en",
                  "timezone":"Asia/Kolkata",
                  "region":"IN",
                  "availabilitySlots":["mon-19:00","wed-19:00"],
                  "groupSharingComfort":"MEDIUM",
                  "structuredAnswers":"{\\"stress\\":\\"high\\"}",
                  "freeText":"I feel anxiety and loneliness recently",
                  "goals":["confidence","support"],
                  "consentAi":true,
                  "consentGroup":true,
                  "notEmergencyAck":true
                }
                """;

        mockMvc.perform(post("/api/v1/intake/submissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(intakeBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.issueTags[0]").value("anxiety"));

        verify(aiService, times(1)).detectRisk(anyString());
        verify(aiService, times(1)).extractFeatures(any());
        verify(aiService, times(1)).embed(anyString());
    }
}
