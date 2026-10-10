package com.irene.irene.deployment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "app.cors.allowed-origins=https://irene-test.web.app,https://irene-test.firebaseapp.com",
    "app.ai.gemini-api-key=synthetic-test-placeholder",
    "app.ai.gemini-model=synthetic-test-model",
    "spring.datasource.url=jdbc:h2:mem:deployment;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.flyway.create-schemas=true"
})
@AutoConfigureMockMvc
@ActiveProfiles({"hosted", "test"})
class DeploymentSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired ConfigurableApplicationContext context;

    @Test void onlyNamedFirebaseOriginsCanPreflightBearerRequests() throws Exception {
        for (String origin : new String[]{"https://irene-test.web.app", "https://irene-test.firebaseapp.com"}) {
            mvc.perform(options("/api/v1/experience/workspace")
                    .header("Origin", origin).header("Access-Control-Request-Method", "GET")
                    .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", origin))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
        }
        mvc.perform(options("/api/v1/experience/workspace")
                .header("Origin", "https://irene-test.web.app.attacker.example")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isForbidden())
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test void corsDoesNotReplaceAuthentication() throws Exception {
        mvc.perform(get("/api/v1/experience/workspace").header("Origin", "https://irene-test.web.app"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/actuator/env")).andExpect(status().isForbidden());
    }

    @Test void healthChecksExposeStatusWithoutInternalDetails() throws Exception {
        mvc.perform(get("/actuator/health/readiness"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.components").doesNotExist());
        mvc.perform(get("/actuator/health/liveness"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/api/v1/health"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.demoAccounts").value("false"));
    }

    @Test void unreadyInstanceIsNotDeclaredDead() throws Exception {
        try {
            AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC);
            mvc.perform(get("/actuator/health/readiness")).andExpect(status().isServiceUnavailable());
            mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        } finally {
            AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
        }
    }

    @Test void apiDocumentationIsDisabledInHostedProfile() throws Exception {
        mvc.perform(get("/api-docs")).andExpect(status().isNotFound());
    }
}
