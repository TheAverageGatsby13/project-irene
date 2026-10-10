package com.irene.irene.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerAndLoginShouldWork() throws Exception {
        String registerBody = """
                {
                  "email": "therapist_test@irene.app",
                  "password": "Password@123",
                  "role": "PATIENT"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("PATIENT"));

        String loginBody = """
                {
                  "email": "therapist_test@irene.app",
                  "password": "Password@123"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void patientCannotCreateTherapistTemplate() throws Exception {
        String patientRegisterBody = """
                {
                  "email": "patient_test@irene.app",
                  "password": "Password@123",
                  "role": "PATIENT"
                }
                """;

        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patientRegisterBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(response);
        String token = node.get("token").asText();

        String templateBody = """
                {
                  "title": "Test Template",
                  "description": "desc",
                  "issueFocus": ["anxiety"],
                  "goalFocus": ["confidence"],
                  "targetSize": 8,
                  "scheduleWindow": ["mon-19:00"],
                  "languageCode": "en",
                  "region": "IN",
                  "targetAgeBand": "18-24",
                  "minGroupSharingComfort": "MEDIUM"
                }
                """;

        mockMvc.perform(post("/api/v1/therapist/templates")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(templateBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicRegistrationCannotGrantPrivilegedRoles() throws Exception {
        for (String role : java.util.List.of("ADMIN", "THERAPIST")) {
            mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"privileged@irene.example\",\"password\":\"Password@123\",\"role\":\"" + role + "\"}"))
                    .andExpect(status().isForbidden());
        }
    }
}
