package com.irene.irene.matching.model;

import com.irene.irene.auth.model.User;
import com.irene.irene.intake.model.IntakeSubmission;
import com.irene.irene.therapist.model.ProgramTemplate;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "match_candidates")
public class MatchCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ProgramTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intake_submission_id", nullable = false)
    private IntakeSubmission intakeSubmission;

    @Column(name = "run_id", nullable = false)
    private String runId;

    @Column(name = "hard_filter_pass", nullable = false)
    private boolean hardFilterPass;

    @Column(nullable = false)
    private double score;

    @Column(nullable = false, length = 4000)
    private String explanation;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
