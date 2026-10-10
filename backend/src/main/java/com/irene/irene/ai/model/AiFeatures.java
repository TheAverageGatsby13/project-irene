package com.irene.irene.ai.model;

import com.irene.irene.intake.model.IntakeSubmission;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

//a blueprint for one DB row that stores AI analysis (tags, goals, risk, summary, embedding) for exactly one intake submission, with validation constraints (required fields + max lengths) baked into the mapping.
@Getter
@Setter
@Entity
@Table(name = "ai_features")
public class AiFeatures {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intake_submission_id", nullable = false, unique = true)
    private IntakeSubmission intakeSubmission;

    @Column(name = "issue_tags", nullable = false, length = 2000)
    private String issueTags;

    @Column(nullable = false, length = 2000)
    private String goals;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    private RiskLevel riskLevel;

    @Column(nullable = false, length = 4000)
    private String summary;

    @Column(nullable = false, length = 12000)
    private String embedding;

    @Column(name = "normalized_text", nullable = false, length = 4000)
    private String normalizedText;

    @Column(name = "embedding_space", nullable = false)
    private String embeddingSpace = "legacy";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
