package com.irene.irene.ai.service;

import com.irene.irene.ai.model.ExtractedFeatures;
import com.irene.irene.ai.model.RiskLevel;
import com.irene.irene.common.util.CsvUtil;
import com.irene.irene.common.util.VectorUtil;
import com.irene.irene.intake.model.IntakeSubmission;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@ConditionalOnProperty(prefix = "app.ai", name = "mode", havingValue = "mock", matchIfMissing = true)
public class MockAiService implements AiService {

    @Override
    public RiskLevel detectRisk(String intakeText) {
        String lower = intakeText.toLowerCase(Locale.ROOT);
        if (lower.contains("suicide") || lower.contains("kill myself") || lower.contains("self harm")) {
            return RiskLevel.HIGH;
        }
        if (lower.contains("panic") || lower.contains("severe") || lower.contains("can't sleep")) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    @Override
    public ExtractedFeatures extractFeatures(IntakeSubmission submission) {
        String freeText = submission.getFreeText();
        String goals = submission.getGoalsText();
        List<String> issues = inferIssueTags(freeText);
        List<String> normalizedGoals = CsvUtil.split(goals);
        if (normalizedGoals.isEmpty()) {
            normalizedGoals = List.of("support", "emotional-regulation");
        }
        String summary = "Patient seeks support for " + String.join(", ", issues) + " with goals: " + String.join(", ", normalizedGoals);
        String normalizedText = "issues=" + String.join("|", issues) + ";goals=" + String.join("|", normalizedGoals);
        return new ExtractedFeatures(issues, normalizedGoals, summary, normalizedText);
    }

    @Override
    public float[] embed(String normalizedText) {
        return VectorUtil.deterministicEmbedding(normalizedText, 12);
    }

    private List<String> inferIssueTags(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        List<String> tags = new ArrayList<>();
        if (lower.contains("anxiety")) {
            tags.add("anxiety");
        }
        if (lower.contains("social")) {
            tags.add("social-anxiety");
        }
        if (lower.contains("lonely") || lower.contains("loneliness")) {
            tags.add("loneliness");
        }
        if (lower.contains("breakup")) {
            tags.add("breakup");
        }
        if (lower.contains("grief") || lower.contains("loss")) {
            tags.add("grief");
        }
        if (tags.isEmpty()) {
            tags.add("stress");
        }
        return tags;
    }
}
