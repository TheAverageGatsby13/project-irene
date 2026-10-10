package com.irene.irene.workflow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static com.irene.irene.workflow.ExperienceDtos.*;
@RestController
@RequestMapping("/api/v1/experience")
@Tag(name="Complete participant and therapist journey")
public class ExperienceController {
    private final ExperienceService service;
    public ExperienceController(ExperienceService service) { this.service=service; }
    @GetMapping("/catalog") @Operation(summary="Browse active verified therapist programs")
    public List<Program> catalog() { return service.catalog(); }
    @GetMapping("/workspace") @Operation(summary="Get only the current user's workspace")
    public Workspace workspace() { return service.workspace(); }
    @PutMapping("/intakes/{id}/confirm") @Operation(summary="Correct and confirm own current intake summary")
    public Message confirm(@PathVariable Long id,@Valid @RequestBody Confirm request) { return service.confirm(id,request); }
    @PutMapping("/intakes/{id}/review") @Operation(summary="Assigned therapist records suitability review")
    public Message screen(@PathVariable Long id,@Valid @RequestBody Screening request) { return service.screen(id,request); }
    @PostMapping("/circles/{id}/approve") @Operation(summary="Approve, schedule and invite a reviewed group")
    public Message approve(@PathVariable Long id,@Valid @RequestBody Approval request) { return service.approve(id,request); }
    @PutMapping("/circles/{id}/response") @Operation(summary="Accept, decline or withdraw own invitation")
    public Message reply(@PathVariable Long id,@Valid @RequestBody Reply request) { return service.reply(id,request); }
    @PostMapping("/circles/{id}/complete") @Operation(summary="Mark an ended session complete")
    public Message complete(@PathVariable Long id) { return service.complete(id); }
    @PostMapping("/circles/{id}/cancel") @Operation(summary="Cancel an owned group")
    public Message cancel(@PathVariable Long id) { return service.cancel(id); }
    @PostMapping("/circles/{id}/brief") @Operation(summary="Generate a private facilitator preparation draft")
    public Message brief(@PathVariable Long id) { return service.brief(id); }
    @PutMapping("/circles/{id}/feedback") @Operation(summary="Save own post-session feedback")
    public Message feedback(@PathVariable Long id,@Valid @RequestBody Feedback request) { return service.feedback(id,request); }
    @PostMapping("/credentials") @Operation(summary="Apply for verified therapist access")
    public Message apply(@Valid @RequestBody Application request) { return service.apply(request); }
    @PutMapping("/credentials/{id}") @Operation(summary="Administrator verifies professional credentials")
    public Message verify(@PathVariable Long id,@Valid @RequestBody Verification request) { return service.verify(id,request); }
    @DeleteMapping("/my-intake") @Operation(summary="Withdraw and delete own intake and derived data")
    public Message delete() { return service.deleteIntake(); }
    @GetMapping("/export") @Operation(summary="Export own current intake and session information")
    public Export export() { return service.export(); }
}
