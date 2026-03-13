package com.irene.irene.intake.model;

import com.irene.irene.auth.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "intake_submissions")
public class IntakeSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @Column(name = "structured_answers", nullable = false, length = 8000)
    private String structuredAnswers;

    @Column(name = "free_text", nullable = false, length = 8000)
    private String freeText;

    @Column(name = "goals_text", nullable = false, length = 2000)
    private String goalsText;

    @Column(name = "consent_ai", nullable = false)
    private boolean consentAi;

    @Column(name = "consent_group", nullable = false)
    private boolean consentGroup;

    @Column(name = "not_emergency_ack", nullable = false)
    private boolean notEmergencyAck;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
