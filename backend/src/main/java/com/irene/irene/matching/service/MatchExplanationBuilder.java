package com.irene.irene.matching.service;

import com.irene.irene.ai.model.AiFeatures;
import com.irene.irene.common.util.CsvUtil;
import com.irene.irene.intake.model.PatientProfile;
import com.irene.irene.therapist.model.ProgramTemplate;
import org.springframework.stereotype.Service;

@Service
public class MatchExplanationBuilder {

    public String build(ProgramTemplate template, PatientProfile profile, AiFeatures aiFeatures, double score) {
        return "Matched for template '" + template.getTitle() + "' with issues " + CsvUtil.split(aiFeatures.getIssueTags()) +
                ", goals " + CsvUtil.split(aiFeatures.getGoals()) +
                ", availability " + CsvUtil.split(profile.getAvailabilitySlots()) +
                ", score=" + String.format("%.3f", score);
    }
}
