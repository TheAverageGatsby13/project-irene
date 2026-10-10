package com.irene.irene.intake.dto;

import com.irene.irene.intake.model.GroupSharingComfort;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record IntakeSubmissionRequest(
        @NotBlank @jakarta.validation.constraints.Pattern(regexp = "18-24|25-34|35-44|45-54|55[+]", message = "Choose a supported adult age band") String ageBand,
        @NotBlank @Size(max=10) String languageCode,
        @NotBlank @Size(max=80) String timezone,
        @NotBlank @Size(max=30) String region,
        @NotEmpty @Size(max=14) List<@NotBlank @Size(max=15) String> availabilitySlots,
        @NotNull GroupSharingComfort groupSharingComfort,
        @NotBlank @Size(max = 8000) String structuredAnswers,
        @NotBlank @Size(max = 8000) String freeText,
        @NotEmpty @Size(max=12) List<@NotBlank @Size(max=100) String> goals,
        boolean consentAi,
        boolean consentGroup,
        boolean notEmergencyAck
) {
}
