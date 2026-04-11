package com.irene.irene.matching.dto;

import java.util.List;

public record MatchRunResponse(
        Long templateId,
        String runId,
        List<MatchCandidateResponse> candidates
) {
}
