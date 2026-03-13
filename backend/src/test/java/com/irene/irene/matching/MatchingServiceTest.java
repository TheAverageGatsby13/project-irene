package com.irene.irene.matching;

import com.irene.irene.ai.model.AiFeatures;
import com.irene.irene.intake.model.GroupSharingComfort;
import com.irene.irene.intake.model.PatientProfile;
import com.irene.irene.matching.service.HardFilterService;
import com.irene.irene.matching.service.SimilarityScorer;
import com.irene.irene.therapist.model.ProgramTemplate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MatchingServiceTest {

    private final HardFilterService hardFilterService = new HardFilterService();
    private final SimilarityScorer similarityScorer = new SimilarityScorer();

    @Test
    void scoreShouldRankBetterOverlapHigher() {
        ProgramTemplate template = new ProgramTemplate();
        template.setIssueFocus("anxiety,social-anxiety,stress");
        template.setGoalFocus("confidence,regulation");
        template.setScheduleWindow("mon-19:00,wed-19:00");
        template.setMinGroupSharingComfort("MEDIUM");

        PatientProfile profileA = new PatientProfile();
        profileA.setAvailabilitySlots("mon-19:00,wed-19:00");
        profileA.setGroupSharingComfort(GroupSharingComfort.HIGH);

        PatientProfile profileB = new PatientProfile();
        profileB.setAvailabilitySlots("sun-08:00");
        profileB.setGroupSharingComfort(GroupSharingComfort.MEDIUM);

        AiFeatures featuresA = new AiFeatures();
        featuresA.setIssueTags("anxiety,social-anxiety");
        featuresA.setGoals("confidence,regulation");
        featuresA.setEmbedding("0.9,0.8,0.7,0.6,0.5,0.4,0.3,0.2,0.1,0.05,0.04,0.03");

        AiFeatures featuresB = new AiFeatures();
        featuresB.setIssueTags("grief");
        featuresB.setGoals("sleep");
        featuresB.setEmbedding("0.1,0.2,0.1,0.1,0.2,0.1,0.1,0.2,0.1,0.1,0.1,0.2");

        double scoreA = similarityScorer.score(template, profileA, featuresA);
        double scoreB = similarityScorer.score(template, profileB, featuresB);

        assertThat(scoreA).isGreaterThan(scoreB);
    }

    @Test
    void hardFilterShouldRejectWhenNoAvailabilityOverlap() {
        ProgramTemplate template = new ProgramTemplate();
        template.setLanguageCode("en");
        template.setRegion("IN");
        template.setTargetAgeBand("18-24");
        template.setScheduleWindow("mon-19:00");
        template.setMinGroupSharingComfort("LOW");

        PatientProfile profile = new PatientProfile();
        profile.setLanguageCode("en");
        profile.setRegion("IN");
        profile.setAgeBand("18-24");
        profile.setAvailabilitySlots("tue-10:00");
        profile.setGroupSharingComfort(GroupSharingComfort.HIGH);

        boolean passed = hardFilterService.passes(template, profile, com.irene.irene.ai.model.RiskLevel.LOW);

        assertThat(passed).isFalse();
    }
}
