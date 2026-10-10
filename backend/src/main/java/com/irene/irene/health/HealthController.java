package com.irene.irene.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health")
public class HealthController {

    @org.springframework.beans.factory.annotation.Value("${app.ai.mode:mock}")
    private String aiMode;

    @org.springframework.beans.factory.annotation.Value("${app.seed-data:false}")
    private boolean demoAccounts;

    @GetMapping
    @Operation(summary = "Application health ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok", "service", "project-irene", "aiMode", aiMode, "demoAccounts", String.valueOf(demoAccounts));
    }
}
