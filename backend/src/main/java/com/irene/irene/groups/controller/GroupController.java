package com.irene.irene.groups.controller;

import com.irene.irene.groups.dto.CreateGroupFromMatchRequest;
import com.irene.irene.groups.dto.GroupResponse;
import com.irene.irene.groups.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/groups")
@Tag(name = "Groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping("/create-from-match")
    @PreAuthorize("hasAnyRole('THERAPIST','ADMIN')")
    @Operation(summary = "Create a therapy group from match run candidates")
    public GroupResponse createFromMatch(@Valid @RequestBody CreateGroupFromMatchRequest request) {
        return groupService.createFromMatch(request);
    }
}
