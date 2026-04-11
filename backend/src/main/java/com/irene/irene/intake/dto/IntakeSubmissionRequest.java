package com.irene.irene.intake.dto;

import com.irene.irene.intake.model.GroupSharingComfort;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record IntakeSubmissionRequest(
        @NotBlank String ageBand,
        @NotBlank String languageCode,
        @NotBlank String timezone,
        @NotBlank String region,
        @NotEmpty List<@NotBlank String> availabilitySlots,
        @NotNull GroupSharingComfort groupSharingComfort,
        @NotBlank @Size(max = 8000) String structuredAnswers,
        @NotBlank @Size(max = 8000) String freeText,
        @NotEmpty List<@NotBlank String> goals,
        boolean consentAi,
        boolean consentGroup,
        boolean notEmergencyAck
) {
}
