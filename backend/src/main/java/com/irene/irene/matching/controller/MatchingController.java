package com.irene.irene.matching.controller;

import com.irene.irene.matching.dto.MatchRunResponse;
import com.irene.irene.matching.service.MatchingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/matching")
@Tag(name = "Matching")
public class MatchingController {

    private final MatchingService matchingService;

    public MatchingController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @PostMapping("/run/{templateId}")
    @PreAuthorize("hasAnyRole('THERAPIST','ADMIN')")
    @Operation(summary = "Run matching for a template")
    public MatchRunResponse run(@PathVariable Long templateId) {
        return matchingService.run(templateId);
    }

    @GetMapping("/results/{templateId}")
    @PreAuthorize("hasAnyRole('THERAPIST','ADMIN')")
    @Operation(summary = "Get latest matching results for a template")
    public MatchRunResponse latest(@PathVariable Long templateId) {
        return matchingService.latestResults(templateId);
    }
}
