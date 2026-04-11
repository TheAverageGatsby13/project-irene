package com.irene.irene.intake.controller;

import com.irene.irene.intake.dto.IntakeSubmissionRequest;
import com.irene.irene.intake.dto.IntakeSubmissionResponse;
import com.irene.irene.intake.service.IntakeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/intake/submissions")
@Tag(name = "Intake")
public class IntakeController {

    private final IntakeService intakeService;

    public IntakeController(IntakeService intakeService) {
        this.intakeService = intakeService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PATIENT','ADMIN')")
    @Operation(summary = "Submit patient intake and run AI extraction")
    public IntakeSubmissionResponse submit(@Valid @RequestBody IntakeSubmissionRequest request) {
        return intakeService.submit(request);
    }
}
