package com.irene.irene.intake.service;

import com.irene.irene.ai.model.AiFeatures;
import com.irene.irene.ai.model.ExtractedFeatures;
import com.irene.irene.ai.model.RiskLevel;
import com.irene.irene.ai.repository.AiFeaturesRepository;
import com.irene.irene.ai.service.AiService;
import com.irene.irene.auth.model.Role;
import com.irene.irene.auth.model.User;
import com.irene.irene.common.security.CurrentUserService;
import com.irene.irene.common.util.CsvUtil;
import com.irene.irene.common.util.VectorUtil;
import com.irene.irene.intake.dto.IntakeSubmissionRequest;
import com.irene.irene.intake.dto.IntakeSubmissionResponse;
import com.irene.irene.intake.model.IntakeSubmission;
import com.irene.irene.intake.model.PatientProfile;
import com.irene.irene.intake.repository.IntakeSubmissionRepository;
import com.irene.irene.intake.repository.PatientProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
public class IntakeService {

    private final CurrentUserService currentUserService;
    private final PatientProfileRepository patientProfileRepository;
    private final IntakeSubmissionRepository intakeSubmissionRepository;
    private final AiFeaturesRepository aiFeaturesRepository;
    private final AiService aiService;

    public IntakeService(CurrentUserService currentUserService,
                         PatientProfileRepository patientProfileRepository,
                         IntakeSubmissionRepository intakeSubmissionRepository,
                         AiFeaturesRepository aiFeaturesRepository,
                         AiService aiService) {
        this.currentUserService = currentUserService;
        this.patientProfileRepository = patientProfileRepository;
        this.intakeSubmissionRepository = intakeSubmissionRepository;
        this.aiFeaturesRepository = aiFeaturesRepository;
        this.aiService = aiService;
    }

    @Transactional
    public IntakeSubmissionResponse submit(IntakeSubmissionRequest request) {
        User currentUser = currentUserService.requireUser();
        if (currentUser.getRole() != Role.PATIENT && currentUser.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(FORBIDDEN, "Only patients can submit intake");
        }

        if (!request.consentAi() || !request.consentGroup() || !request.notEmergencyAck()) {
            throw new ResponseStatusException(BAD_REQUEST, "AI consent, group consent, and not-emergency acknowledgement are required");
        }

        PatientProfile profile = patientProfileRepository.findByUserId(currentUser.getId())
                .orElseGet(PatientProfile::new);
        profile.setUser(currentUser);
        profile.setAgeBand(request.ageBand().trim());
        profile.setLanguageCode(request.languageCode().trim().toLowerCase());
        profile.setTimezone(request.timezone().trim());
        profile.setRegion(request.region().trim());
        profile.setAvailabilitySlots(CsvUtil.join(request.availabilitySlots()));
        profile.setGroupSharingComfort(request.groupSharingComfort());
        patientProfileRepository.save(profile);

        IntakeSubmission submission = new IntakeSubmission();
        submission.setPatient(currentUser);
        submission.setStructuredAnswers(request.structuredAnswers().trim());
        submission.setFreeText(request.freeText().trim());
        submission.setGoalsText(CsvUtil.join(request.goals()));
        submission.setConsentAi(request.consentAi());
        submission.setConsentGroup(request.consentGroup());
        submission.setNotEmergencyAck(request.notEmergencyAck());
        IntakeSubmission savedSubmission = intakeSubmissionRepository.save(submission);

        RiskLevel riskLevel = aiService.detectRisk(savedSubmission.getFreeText());
        ExtractedFeatures extracted = aiService.extractFeatures(savedSubmission);
        float[] vector = aiService.embed(extracted.normalizedText());

        AiFeatures features = new AiFeatures();
        features.setIntakeSubmission(savedSubmission);
        features.setIssueTags(CsvUtil.join(extracted.issueTags()));
        features.setGoals(CsvUtil.join(extracted.goals()));
        features.setRiskLevel(riskLevel);
        features.setSummary(extracted.summary());
        features.setEmbedding(VectorUtil.serialize(vector));
        features.setNormalizedText(extracted.normalizedText());
        aiFeaturesRepository.save(features);

        boolean routedToManualReview = riskLevel == RiskLevel.HIGH;

        return new IntakeSubmissionResponse(
                savedSubmission.getId(),
                riskLevel,
                extracted.issueTags(),
                extracted.goals(),
                extracted.summary(),
                routedToManualReview
        );
    }
}
