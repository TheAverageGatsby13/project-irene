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

    @GetMapping
    @Operation(summary = "Application health ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok", "service", "project-irene");
    }
}
