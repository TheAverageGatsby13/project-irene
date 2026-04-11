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
                        GroupMembershipRepository groupMembershipRepository) {
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
        if (actor.getRole() != Role.THERAPIST && actor.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(FORBIDDEN, "Only therapists can create groups");
        }

        ProgramTemplate template = programTemplateService.getEntity(request.templateId());
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
            groupMembershipRepository.save(membership);
            createdPatientIds.add(patientId);
        }

        return new GroupResponse(savedGroup.getId(), template.getId(), savedGroup.getTitle(), savedGroup.getStatus().name(), createdPatientIds);
    }
}
