package com.irene.irene.groups.service;

import com.irene.irene.auth.model.Role;
import com.irene.irene.auth.model.User;
import com.irene.irene.auth.repository.UserRepository;
import com.irene.irene.common.security.CurrentUserService;
import com.irene.irene.groups.dto.CreateGroupFromMatchRequest;
import com.irene.irene.groups.dto.GroupResponse;
import com.irene.irene.groups.model.GroupMembership;
import com.irene.irene.groups.model.MembershipStatus;
import com.irene.irene.groups.model.TherapyGroup;
import com.irene.irene.groups.model.TherapyGroupStatus;
import com.irene.irene.groups.repository.GroupMembershipRepository;
import com.irene.irene.groups.repository.TherapyGroupRepository;
import com.irene.irene.matching.repository.MatchCandidateRepository;
import com.irene.irene.therapist.model.ProgramTemplate;
import com.irene.irene.therapist.service.ProgramTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class GroupService {

    private final com.irene.irene.intake.repository.IntakeSubmissionRepository intakes;
    private final CurrentUserService currentUserService;
    private final ProgramTemplateService programTemplateService;
    private final MatchCandidateRepository matchCandidateRepository;
    private final UserRepository userRepository;
    private final TherapyGroupRepository therapyGroupRepository;
    private final GroupMembershipRepository groupMembershipRepository;

    public GroupService(CurrentUserService currentUserService,
                        ProgramTemplateService programTemplateService,
                        MatchCandidateRepository matchCandidateRepository,
                        UserRepository userRepository,
                        TherapyGroupRepository therapyGroupRepository,
                        GroupMembershipRepository groupMembershipRepository,
                        com.irene.irene.intake.repository.IntakeSubmissionRepository intakes) {
        this.intakes = intakes;
        this.currentUserService = currentUserService;
        this.programTemplateService = programTemplateService;
        this.matchCandidateRepository = matchCandidateRepository;
        this.userRepository = userRepository;
        this.therapyGroupRepository = therapyGroupRepository;
        this.groupMembershipRepository = groupMembershipRepository;
    }

    @Transactional
    public GroupResponse createFromMatch(CreateGroupFromMatchRequest request) {
        User actor = currentUserService.requireUser();
        if (actor.getRole() != Role.THERAPIST) {
            throw new ResponseStatusException(FORBIDDEN, "Only therapists can create groups");
        }

        ProgramTemplate template = programTemplateService.getEntity(request.templateId());
        if (!template.isActive()) throw new ResponseStatusException(BAD_REQUEST, "Program is inactive");
        if (request.patientIds().size() < 2 || request.patientIds().size() > template.getTargetSize()
                || new java.util.HashSet<>(request.patientIds()).size() != request.patientIds().size()) {
            throw new ResponseStatusException(BAD_REQUEST, "Choose distinct participants within the program capacity (minimum two)");
        }
        var candidates = matchCandidateRepository.findByTemplateIdAndRunIdOrderByScoreDesc(request.templateId(), request.runId());
        if (candidates.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid runId or templateId");
        }

        List<Long> matchedIds = candidates.stream().map(c -> c.getPatient().getId()).toList();
        for (Long patientId : request.patientIds()) {
            if (!matchedIds.contains(patientId)) {
                throw new ResponseStatusException(BAD_REQUEST, "Patient " + patientId + " not found in selected run");
            }
        }

        for (Long id : request.patientIds()) {
            var candidate = candidates.stream().filter(c -> c.getPatient().getId().equals(id)).findFirst().orElseThrow();
            var latest = intakes.findTopByPatientIdOrderByCreatedAtDesc(id).orElseThrow();
            if (!latest.isSummaryConfirmed() || !"APPROVED".equals(latest.getReviewStatus())
                    || !template.getId().equals(latest.getProgramId())
                    || !template.getTherapist().getId().equals(latest.getAssignedTherapistId())) {
                throw new ResponseStatusException(BAD_REQUEST, "Every participant needs a current suitability review");
            }
            if (!candidate.isHardFilterPass() || !latest.getId().equals(candidate.getIntakeSubmission().getId())
                    || !latest.isConsentAi() || !latest.isConsentGroup() || !latest.isNotEmergencyAck()) {
                throw new ResponseStatusException(BAD_REQUEST, "Intake changed or consent is missing; run matching again");
            }
        }

        TherapyGroup group = new TherapyGroup();
        group.setTemplate(template);
        group.setTherapist(actor);
        group.setTitle(request.title().trim());
        group.setStatus(TherapyGroupStatus.PENDING_APPROVAL);
        TherapyGroup savedGroup = therapyGroupRepository.save(group);

        List<Long> createdPatientIds = new ArrayList<>();
        for (Long patientId : request.patientIds()) {
            User patient = userRepository.findById(patientId)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Patient not found: " + patientId));

            GroupMembership membership = new GroupMembership();
            membership.setGroup(savedGroup);
            membership.setPatient(patient);
            membership.setStatus(MembershipStatus.INVITED);
            membership.setIntakeId(intakes.findTopByPatientIdOrderByCreatedAtDesc(patientId).orElseThrow().getId());
            groupMembershipRepository.save(membership);
            createdPatientIds.add(patientId);
        }

        return new GroupResponse(savedGroup.getId(), template.getId(), savedGroup.getTitle(), savedGroup.getStatus().name(), createdPatientIds);
    }
}
