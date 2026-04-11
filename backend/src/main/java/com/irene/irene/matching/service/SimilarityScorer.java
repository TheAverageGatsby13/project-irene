package com.irene.irene.matching.service;

import com.irene.irene.ai.model.AiFeatures;
import com.irene.irene.common.util.CsvUtil;
import com.irene.irene.common.util.VectorUtil;
import com.irene.irene.intake.model.GroupSharingComfort;
import com.irene.irene.intake.model.PatientProfile;
import com.irene.irene.therapist.model.ProgramTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SimilarityScorer {

    public double score(ProgramTemplate template, PatientProfile profile, AiFeatures aiFeatures) {
        double issueSimilarity = issueSimilarity(template, aiFeatures);
        double goalSimilarity = jaccard(CsvUtil.split(template.getGoalFocus()), CsvUtil.split(aiFeatures.getGoals()));
        double availabilityOverlap = overlapRatio(CsvUtil.split(template.getScheduleWindow()), CsvUtil.split(profile.getAvailabilitySlots()));
        double preferenceFit = preferenceFit(template.getMinGroupSharingComfort(), profile.getGroupSharingComfort());

        return 0.45 * issueSimilarity + 0.25 * goalSimilarity + 0.20 * availabilityOverlap + 0.10 * preferenceFit;
    }

    private double issueSimilarity(ProgramTemplate template, AiFeatures aiFeatures) {
        List<String> templateIssues = CsvUtil.split(template.getIssueFocus());
        List<String> candidateIssues = CsvUtil.split(aiFeatures.getIssueTags());
        double tagSim = jaccard(templateIssues, candidateIssues);

        float[] templateVector = VectorUtil.deterministicEmbedding(String.join("|", templateIssues), 12);
        float[] candidateVector = VectorUtil.deserialize(aiFeatures.getEmbedding());
        double vectorSim = Math.max(0, VectorUtil.cosineSimilarity(templateVector, candidateVector));

        return 0.7 * tagSim + 0.3 * vectorSim;
    }

    private double jaccard(List<String> a, List<String> b) {
        if (a.isEmpty() && b.isEmpty()) {
            return 1.0;
        }
        Set<String> setA = new HashSet<>(a);
        Set<String> setB = new HashSet<>(b);
        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);

        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);

        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }

    private double overlapRatio(List<String> a, List<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        Set<String> setA = new HashSet<>(a);
        Set<String> setB = new HashSet<>(b);
        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);

        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);

        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }

    private double preferenceFit(String minComfort, GroupSharingComfort patientComfort) {
        GroupSharingComfort minimum = switch (minComfort.toUpperCase()) {
            case "HIGH" -> GroupSharingComfort.HIGH;
            case "MEDIUM" -> GroupSharingComfort.MEDIUM;
            default -> GroupSharingComfort.LOW;
        };
        if (patientComfort.ordinal() < minimum.ordinal()) {
            return 0;
        }
        return switch (patientComfort) {
            case LOW -> 0.7;
            case MEDIUM -> 0.85;
            case HIGH -> 1.0;
        };
    }
}
