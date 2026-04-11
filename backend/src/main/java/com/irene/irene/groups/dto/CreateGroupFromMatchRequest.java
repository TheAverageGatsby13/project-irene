package com.irene.irene.groups.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateGroupFromMatchRequest(
        @NotNull Long templateId,
        @NotBlank String runId,
        @NotBlank String title,
        @NotEmpty List<@NotNull Long> patientIds
) {
}
