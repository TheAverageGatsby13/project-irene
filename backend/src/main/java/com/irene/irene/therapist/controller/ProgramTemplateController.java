package com.irene.irene.therapist.controller;

import com.irene.irene.therapist.dto.CreateProgramTemplateRequest;
import com.irene.irene.therapist.dto.ProgramTemplateResponse;
import com.irene.irene.therapist.service.ProgramTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/therapist/templates")
@Tag(name = "Therapist Templates")
public class ProgramTemplateController {

    private final ProgramTemplateService service;

    public ProgramTemplateController(ProgramTemplateService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('THERAPIST','ADMIN')")
    @Operation(summary = "Create therapist program template")
    public ProgramTemplateResponse create(@Valid @RequestBody CreateProgramTemplateRequest request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('THERAPIST','ADMIN')")
    @Operation(summary = "Get program template by id")
    public ProgramTemplateResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }
}
