package com.irene.irene.therapist.dto;

import java.util.List;

public record ProgramTemplateResponse(
        Long id,
        Long therapistId,
        String title,
        String description,
        List<String> issueFocus,
        List<String> goalFocus,
        Integer targetSize,
        List<String> scheduleWindow,
        String languageCode,
        String region,
        String targetAgeBand,
        String minGroupSharingComfort,
        boolean active
) {
}
