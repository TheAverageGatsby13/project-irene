package com.irene.irene.ai.service;

import com.irene.irene.ai.model.ExtractedFeatures;
import com.irene.irene.ai.model.RiskLevel;
import com.irene.irene.intake.model.IntakeSubmission;

public interface AiService {
    RiskLevel detectRisk(String intakeText);
    ExtractedFeatures extractFeatures(IntakeSubmission submission);
    float[] embed(String normalizedText);
}
