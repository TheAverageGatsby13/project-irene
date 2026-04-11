package com.irene.irene.intake.repository;

import com.irene.irene.intake.model.IntakeSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IntakeSubmissionRepository extends JpaRepository<IntakeSubmission, Long> {
    Optional<IntakeSubmission> findTopByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<IntakeSubmission> findByPatientId(Long patientId);
}
