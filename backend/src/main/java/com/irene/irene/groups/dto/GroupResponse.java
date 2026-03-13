package com.irene.irene.groups.dto;

import java.util.List;

public record GroupResponse(
        Long groupId,
        Long templateId,
        String title,
        String status,
        List<Long> patientIds
) {
}
