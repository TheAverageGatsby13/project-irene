package com.irene.irene.groups;
import com.irene.irene.auth.model.*;
import com.irene.irene.auth.repository.UserRepository;
import com.irene.irene.common.security.CurrentUserService;
import com.irene.irene.groups.dto.CreateGroupFromMatchRequest;
import com.irene.irene.groups.repository.*;
import com.irene.irene.groups.service.GroupService;
import com.irene.irene.intake.repository.IntakeSubmissionRepository;
import com.irene.irene.matching.repository.MatchCandidateRepository;
import com.irene.irene.therapist.model.ProgramTemplate;
import com.irene.irene.therapist.service.ProgramTemplateService;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class GroupProposalTest {
    @Test void duplicateAndOverCapacitySelectionsCannotPersistGroups() {
        var current = mock(CurrentUserService.class);
        var programs = mock(ProgramTemplateService.class);
        var groups = mock(TherapyGroupRepository.class);
        User therapist = new User(); therapist.setRole(Role.THERAPIST);
        when(current.requireUser()).thenReturn(therapist);
        ProgramTemplate template = new ProgramTemplate(); template.setTargetSize(3); template.setActive(true);
        when(programs.getEntity(1L)).thenReturn(template);
        var service = new GroupService(current, programs, mock(MatchCandidateRepository.class), mock(UserRepository.class), groups,
                mock(GroupMembershipRepository.class), mock(IntakeSubmissionRepository.class));
        for (List<Long> ids : List.of(List.of(1L), List.of(1L,1L), List.of(1L,2L,3L,4L))) {
            assertThatThrownBy(() -> service.createFromMatch(new CreateGroupFromMatchRequest(1L, "run", "fictional group", ids)))
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        }
        verifyNoInteractions(groups);
    }
}
