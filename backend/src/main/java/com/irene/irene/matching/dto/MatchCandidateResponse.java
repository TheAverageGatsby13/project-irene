package com.irene.irene.matching.dto;

import com.irene.irene.ai.model.RiskLevel;

import java.util.List;

public record MatchCandidateResponse(
        Long patientId,
        String patientEmail,
        RiskLevel riskLevel,
        List<String> issueTags,
        double score,
        String explanation
) {
}
