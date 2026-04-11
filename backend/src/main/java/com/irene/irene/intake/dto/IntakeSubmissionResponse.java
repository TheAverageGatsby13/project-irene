package com.irene.irene.intake.dto;

import com.irene.irene.ai.model.RiskLevel;

import java.util.List;

public record IntakeSubmissionResponse(
        Long intakeSubmissionId,
        RiskLevel riskLevel,
        List<String> issueTags,
        List<String> goals,
        String summary,
        boolean routedToManualReview
) {
}
