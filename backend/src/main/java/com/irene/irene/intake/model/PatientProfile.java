package com.irene.irene.intake.model;

import com.irene.irene.auth.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "patient_profiles")
public class PatientProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "age_band", nullable = false)
    private String ageBand;

    @Column(name = "language_code", nullable = false)
    private String languageCode;

    @Column(nullable = false)
    private String timezone;

    @Column(nullable = false)
    private String region;

    @Column(name = "availability_slots", nullable = false, length = 2000)
    private String availabilitySlots;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_sharing_comfort", nullable = false)
    private GroupSharingComfort groupSharingComfort;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
