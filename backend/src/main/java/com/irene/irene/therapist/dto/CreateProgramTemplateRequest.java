package com.irene.irene.therapist.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateProgramTemplateRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 2000) String description,
        @NotEmpty List<@NotBlank String> issueFocus,
        @NotEmpty List<@NotBlank String> goalFocus,
        @Min(3) @Max(15) Integer targetSize,
        @NotEmpty List<@NotBlank String> scheduleWindow,
        @NotBlank String languageCode,
        @NotBlank String region,
        @NotBlank String targetAgeBand,
        @NotBlank String minGroupSharingComfort
) {
}
