package com.irene.irene.therapist.service;

import com.irene.irene.auth.model.Role;
import com.irene.irene.auth.model.User;
import com.irene.irene.common.security.CurrentUserService;
import com.irene.irene.common.util.CsvUtil;
import com.irene.irene.therapist.dto.CreateProgramTemplateRequest;
import com.irene.irene.therapist.dto.ProgramTemplateResponse;
import com.irene.irene.therapist.model.ProgramTemplate;
import com.irene.irene.therapist.repository.ProgramTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ProgramTemplateService {

    private final ProgramTemplateRepository repository;
    private final CurrentUserService currentUserService;

    public ProgramTemplateService(ProgramTemplateRepository repository, CurrentUserService currentUserService) {
        this.repository = repository;
        this.currentUserService = currentUserService;
    }

    public ProgramTemplateResponse create(CreateProgramTemplateRequest request) {
        User therapist = currentUserService.requireUser();
        if (therapist.getRole() != Role.THERAPIST && therapist.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(FORBIDDEN, "Only therapists can create program templates");
        }

        ProgramTemplate template = new ProgramTemplate();
        template.setTherapist(therapist);
        template.setTitle(request.title().trim());
        template.setDescription(request.description().trim());
        template.setIssueFocus(CsvUtil.join(request.issueFocus()));
        template.setGoalFocus(CsvUtil.join(request.goalFocus()));
        template.setTargetSize(request.targetSize());
        template.setScheduleWindow(CsvUtil.join(request.scheduleWindow()));
        template.setLanguageCode(request.languageCode().trim().toLowerCase());
        template.setRegion(request.region().trim());
        template.setTargetAgeBand(request.targetAgeBand().trim());
        template.setMinGroupSharingComfort(request.minGroupSharingComfort().trim().toUpperCase());

        return toResponse(repository.save(template));
    }

    public ProgramTemplateResponse getById(Long id) {
        ProgramTemplate template = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Template not found"));
        return toResponse(template);
    }

    public ProgramTemplate getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Template not found"));
    }

    public ProgramTemplateResponse toResponse(ProgramTemplate template) {
        return new ProgramTemplateResponse(
                template.getId(),
                template.getTherapist().getId(),
                template.getTitle(),
                template.getDescription(),
                CsvUtil.split(template.getIssueFocus()),
                CsvUtil.split(template.getGoalFocus()),
                template.getTargetSize(),
                CsvUtil.split(template.getScheduleWindow()),
                template.getLanguageCode(),
                template.getRegion(),
                template.getTargetAgeBand(),
                template.getMinGroupSharingComfort(),
                template.isActive()
        );
    }
}
