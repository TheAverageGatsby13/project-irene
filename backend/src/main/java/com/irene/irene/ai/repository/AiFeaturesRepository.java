package com.irene.irene.ai.repository;

import com.irene.irene.ai.model.AiFeatures;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiFeaturesRepository extends JpaRepository<AiFeatures, Long> {
    Optional<AiFeatures> findByIntakeSubmissionId(Long intakeSubmissionId);
}
