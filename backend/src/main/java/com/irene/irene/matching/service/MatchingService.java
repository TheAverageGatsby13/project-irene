package com.irene.irene.matching.service;

import com.irene.irene.ai.model.AiFeatures;
import com.irene.irene.ai.repository.AiFeaturesRepository;
import com.irene.irene.auth.model.Role;
import com.irene.irene.auth.model.User;
import com.irene.irene.common.security.CurrentUserService;
import com.irene.irene.common.util.CsvUtil;
import com.irene.irene.intake.model.IntakeSubmission;
import com.irene.irene.intake.model.PatientProfile;
import com.irene.irene.intake.repository.IntakeSubmissionRepository;
import com.irene.irene.intake.repository.PatientProfileRepository;
import com.irene.irene.matching.dto.MatchCandidateResponse;
import com.irene.irene.matching.dto.MatchRunResponse;
import com.irene.irene.matching.model.MatchCandidate;
import com.irene.irene.matching.repository.MatchCandidateRepository;
import com.irene.irene.therapist.model.ProgramTemplate;
import com.irene.irene.therapist.service.ProgramTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class MatchingService {

    private final com.irene.irene.ai.service.AiService aiService;
    private final ProgramTemplateService programTemplateService;
    private final IntakeSubmissionRepository intakeSubmissionRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final AiFeaturesRepository aiFeaturesRepository;
    private final MatchCandidateRepository matchCandidateRepository;
    private final HardFilterService hardFilterService;
    private final SimilarityScorer similarityScorer;
    private final MatchExplanationBuilder explanationBuilder;
    private final CurrentUserService currentUserService;

    public MatchingService(ProgramTemplateService programTemplateService,
                           IntakeSubmissionRepository intakeSubmissionRepository,
                           PatientProfileRepository patientProfileRepository,
                           AiFeaturesRepository aiFeaturesRepository,
                           MatchCandidateRepository matchCandidateRepository,
                           HardFilterService hardFilterService,
                           SimilarityScorer similarityScorer,
                           MatchExplanationBuilder explanationBuilder,
                           CurrentUserService currentUserService, com.irene.irene.ai.service.AiService aiService) {
        this.aiService = aiService;
        this.programTemplateService = programTemplateService;
        this.intakeSubmissionRepository = intakeSubmissionRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.aiFeaturesRepository = aiFeaturesRepository;
        this.matchCandidateRepository = matchCandidateRepository;
        this.hardFilterService = hardFilterService;
        this.similarityScorer = similarityScorer;
        this.explanationBuilder = explanationBuilder;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public MatchRunResponse run(Long templateId) {
        User user = currentUserService.requireUser();
        if (user.getRole() != Role.THERAPIST) {
            throw new ResponseStatusException(FORBIDDEN, "Only therapists can run matching");
        }

        ProgramTemplate template = programTemplateService.getEntity(templateId);
        float[] templateVector = aiService.embed("issues=" + template.getIssueFocus() + ";goals=" + template.getGoalFocus());
        Map<Long, IntakeSubmission> latestByPatient = latestIntakesByPatient();
        String runId = UUID.randomUUID().toString().substring(0, 12);
        List<MatchCandidate> persisted = new ArrayList<>();

        for (IntakeSubmission intake : latestByPatient.values()) {
            if (!intake.isSummaryConfirmed() || !"APPROVED".equals(intake.getReviewStatus())
                    || !template.getId().equals(intake.getProgramId())
                    || !template.getTherapist().getId().equals(intake.getAssignedTherapistId())
                    || !intake.isConsentAi() || !intake.isConsentGroup()) continue;
            PatientProfile profile = patientProfileRepository.findByUserId(intake.getPatient().getId()).orElse(null);
            AiFeatures aiFeatures = aiFeaturesRepository.findByIntakeSubmissionId(intake.getId()).orElse(null);
            if (profile == null || aiFeatures == null) {
                continue;
            }

            boolean pass = hardFilterService.passes(template, profile, aiFeatures.getRiskLevel());
            if (!pass) {
                continue;
            }

            double score = similarityScorer.score(template, profile, aiFeatures, aiService.embeddingSpace().equals(aiFeatures.getEmbeddingSpace()) ? templateVector : new float[0]);
            String explanation = explanationBuilder.build(template, profile, aiFeatures, score);

            MatchCandidate candidate = new MatchCandidate();
            candidate.setTemplate(template);
            candidate.setPatient(intake.getPatient());
            candidate.setIntakeSubmission(intake);
            candidate.setRunId(runId);
            candidate.setHardFilterPass(true);
            candidate.setScore(score);
            candidate.setExplanation(explanation);
            persisted.add(matchCandidateRepository.save(candidate));
        }

        List<MatchCandidateResponse> responses = persisted.stream()
                .sorted(Comparator.comparingDouble(MatchCandidate::getScore).reversed())
                .map(this::toResponse)
                .toList();

        return new MatchRunResponse(templateId, runId, responses);
    }

    @Transactional(readOnly = true)
    public MatchRunResponse latestResults(Long templateId) {
        programTemplateService.getEntity(templateId);
        List<MatchCandidate> candidates = matchCandidateRepository.findByTemplateIdOrderByCreatedAtDesc(templateId);
        if (candidates.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "No matching results found for template");
        }
        String runId = candidates.get(0).getRunId();
        List<MatchCandidateResponse> responses = matchCandidateRepository
                .findByTemplateIdAndRunIdOrderByScoreDesc(templateId, runId)
                .stream()
                .map(this::toResponse)
                .toList();
        return new MatchRunResponse(templateId, runId, responses);
    }

    private MatchCandidateResponse toResponse(MatchCandidate candidate) {
        AiFeatures aiFeatures = aiFeaturesRepository.findByIntakeSubmissionId(candidate.getIntakeSubmission().getId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "AI features not found for candidate"));
        return new MatchCandidateResponse(
                candidate.getPatient().getId(),
                "Participant " + candidate.getPatient().getId(),
                aiFeatures.getRiskLevel(),
                CsvUtil.split(aiFeatures.getIssueTags()),
                candidate.getScore(),
                candidate.getExplanation()
        );
    }

    private Map<Long, IntakeSubmission> latestIntakesByPatient() {
        List<IntakeSubmission> all = intakeSubmissionRepository.findAll();
        Map<Long, IntakeSubmission> latest = new HashMap<>();
        for (IntakeSubmission intake : all) {
            Long patientId = intake.getPatient().getId();
            IntakeSubmission existing = latest.get(patientId);
            if (existing == null || intake.getCreatedAt().isAfter(existing.getCreatedAt())) {
                latest.put(patientId, intake);
            }
        }
        return latest;
    }
}
