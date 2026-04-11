package com.irene.irene.matching.repository;

import com.irene.irene.matching.model.MatchCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchCandidateRepository extends JpaRepository<MatchCandidate, Long> {
    List<MatchCandidate> findByTemplateIdAndRunIdOrderByScoreDesc(Long templateId, String runId);
    List<MatchCandidate> findByTemplateIdOrderByCreatedAtDesc(Long templateId);
}
