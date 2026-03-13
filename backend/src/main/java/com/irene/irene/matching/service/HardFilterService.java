package com.irene.irene.matching.service;

import com.irene.irene.ai.model.RiskLevel;
import com.irene.irene.common.util.CsvUtil;
import com.irene.irene.intake.model.GroupSharingComfort;
import com.irene.irene.intake.model.PatientProfile;
import com.irene.irene.therapist.model.ProgramTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class HardFilterService {

    public boolean passes(ProgramTemplate template, PatientProfile profile, RiskLevel riskLevel) {
        if (!template.getLanguageCode().equalsIgnoreCase(profile.getLanguageCode())) {
            return false;
        }
        if (!template.getRegion().equalsIgnoreCase(profile.getRegion())) {
            return false;
        }
        if (!template.getTargetAgeBand().equalsIgnoreCase(profile.getAgeBand())) {
            return false;
        }
        if (riskLevel == RiskLevel.HIGH) {
            return false;
        }

        List<String> templateSlots = CsvUtil.split(template.getScheduleWindow());
        List<String> patientSlots = CsvUtil.split(profile.getAvailabilitySlots());
        long overlap = templateSlots.stream().filter(patientSlots::contains).count();
        if (overlap == 0) {
            return false;
        }

        GroupSharingComfort minComfort = parseComfort(template.getMinGroupSharingComfort());
        return profile.getGroupSharingComfort().ordinal() >= minComfort.ordinal();
    }

    GroupSharingComfort parseComfort(String value) {
        if (value == null || value.isBlank()) {
            return GroupSharingComfort.LOW;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "HIGH" -> GroupSharingComfort.HIGH;
            case "MEDIUM" -> GroupSharingComfort.MEDIUM;
            default -> GroupSharingComfort.LOW;
        };
    }
}
