package com.irene.irene.workflow;

import jakarta.validation.constraints.*;
import java.util.List;

public final class ExperienceDtos {
    private ExperienceDtos() {}
    public record Program(Long id, Long therapistId, String title, String description, String therapist, String language,
                          String timezone, String schedule, int capacity, String ageBand, String region) {}
    public record Intake(Long id, String concerns, String summary, List<String> tags, List<String> goals,
                         String risk, boolean confirmed, String reviewStatus, String reviewNote, Long programId, int budgetMinor) {}
    public record Review(Long intakeId, String participant, String summary, String risk, String status,
                         String program, int budgetMinor) {}
    public record Member(Long participantId, String alias, String status) {}
    public record Circle(Long id, String title, String program, String schedule, String timezone, String therapist, String status, String startsAt,
                         int durationMinutes, Integer priceMinor, String response, String meetingUrl,
                         String brief, int accepted, int invited, List<Member> members, boolean feedbackSubmitted) {}
    public record Credential(Long userId, String name, String qualification, String reference, String region, String status, String note) {}
    public record Workspace(String role, Intake intake, List<Program> programs, List<Review> reviews,
                            List<Circle> circles, List<Credential> credentials) {}
    public record Confirm(@NotNull Long programId, @NotBlank @Size(max=1500) String summary,
                          @Min(0) @Max(10000000) int budgetMinor) {}
    public record Screening(@NotNull @Pattern(regexp="APPROVED|INDIVIDUAL") String decision,
                            @NotBlank @Size(max=1000) String note) {}
    public record Approval(@NotBlank String startsAt, @Min(30) @Max(120) int durationMinutes,
                           @Min(0) @Max(10000000) int totalFeeMinor,
                           @NotBlank @Size(max=400) String meetingUrl, boolean suitabilityConfirmed) {}
    public record Reply(@Pattern(regexp="CONFIRMED|DECLINED") @NotNull String response) {}
    public record Application(@NotBlank @Size(max=100) String name, @NotBlank @Size(max=300) String qualification,
                              @NotBlank @Size(max=200) String reference, @NotBlank @Size(max=80) String region) {}
    public record Verification(@Pattern(regexp="VERIFIED|REJECTED") @NotNull String decision,
                               @NotBlank @Size(max=500) String note, boolean evidenceReviewed) {}
    public record Feedback(@Min(1) @Max(5) int rating, @Size(max=2000) @NotNull String comment) {}
    public record Message(String message) {}
    public record Profile(String ageBand, String language, String timezone, String region, String availability, String sharingComfort) {}
    public record FeedbackRecord(Long groupId, int rating, String comment) {}
    public record Export(List<Intake> intakeHistory, Profile profile, List<Circle> circles, List<FeedbackRecord> feedback) {}
}
