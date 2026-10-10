package com.irene.irene.therapist;
import com.irene.irene.auth.model.*;
import com.irene.irene.common.security.CurrentUserService;
import com.irene.irene.therapist.model.ProgramTemplate;
import com.irene.irene.therapist.repository.ProgramTemplateRepository;
import com.irene.irene.therapist.service.ProgramTemplateService;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class ProgramOwnershipTest {
    @Test void onlyOwnerCanReadAProgram() {
        var repository = mock(ProgramTemplateRepository.class);
        var currentUser = mock(CurrentUserService.class);
        var service = new ProgramTemplateService(repository, currentUser);
        User owner = new User(); owner.setId(1L); owner.setRole(Role.THERAPIST);
        User other = new User(); other.setId(2L); other.setRole(Role.THERAPIST);
        ProgramTemplate template = new ProgramTemplate(); template.setId(10L); template.setTherapist(owner);
        when(repository.findById(10L)).thenReturn(Optional.of(template));
        when(currentUser.requireUser()).thenReturn(other);
        assertThatThrownBy(() -> service.getEntity(10L)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        when(currentUser.requireUser()).thenReturn(owner);
        assertThat(service.getEntity(10L)).isSameAs(template);
        other.setRole(Role.ADMIN); when(currentUser.requireUser()).thenReturn(other);
        assertThatThrownBy(() -> service.getEntity(10L)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
}
