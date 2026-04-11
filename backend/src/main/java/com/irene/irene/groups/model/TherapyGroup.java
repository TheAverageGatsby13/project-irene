package com.irene.irene.groups.model;

import com.irene.irene.auth.model.User;
import com.irene.irene.therapist.model.ProgramTemplate;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "therapy_groups")
public class TherapyGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ProgramTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "therapist_id", nullable = false)
    private User therapist;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TherapyGroupStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
