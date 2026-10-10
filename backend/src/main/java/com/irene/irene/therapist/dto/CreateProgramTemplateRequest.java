package com.irene.irene.therapist.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record CreateProgramTemplateRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 2000) String description,
        @NotEmpty @Size(max=12) List<@NotBlank @Size(max=80) String> issueFocus,
        @NotEmpty @Size(max=12) List<@NotBlank @Size(max=80) String> goalFocus,
        @NotNull @Min(3) @Max(15) Integer targetSize,
        @NotEmpty @Size(max=14) List<@Pattern(regexp="(mon|tue|wed|thu|fri|sat|sun)-([01][0-9]|2[0-3]):[0-5][0-9]") @NotBlank String> scheduleWindow,
        @NotBlank @Size(max=10) String languageCode,
        @NotBlank @Size(max=30) String region,
        @NotBlank @Pattern(regexp="18-24|25-34|35-44|45-54|55[+]") String targetAgeBand,
        @NotBlank @Pattern(regexp="LOW|MEDIUM|HIGH") String minGroupSharingComfort
) {
}
