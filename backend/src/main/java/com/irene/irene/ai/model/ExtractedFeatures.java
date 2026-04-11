package com.irene.irene.ai.model;

import java.util.List;

public record ExtractedFeatures(
        List<String> issueTags,
        List<String> goals,
        String summary,
        String normalizedText
) {
}
