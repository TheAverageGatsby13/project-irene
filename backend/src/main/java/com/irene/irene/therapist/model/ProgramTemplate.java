package com.irene.irene.therapist.model;

import com.irene.irene.auth.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "program_templates")
public class ProgramTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "therapist_id", nullable = false)
    private User therapist;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "issue_focus", nullable = false, length = 2000)
    private String issueFocus;

    @Column(name = "goal_focus", nullable = false, length = 2000)
    private String goalFocus;

    @Column(name = "target_size", nullable = false)
    private Integer targetSize;

    @Column(name = "schedule_window", nullable = false, length = 2000)
    private String scheduleWindow;

    @Column(name = "language_code", nullable = false)
    private String languageCode;

    @Column(nullable = false)
    private String region;

    @Column(name = "target_age_band", nullable = false)
    private String targetAgeBand;

    @Column(name = "min_group_sharing_comfort", nullable = false)
    private String minGroupSharingComfort;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
