package com.psybergate.dap.repository;

import com.psybergate.dap.domain.CodingResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CodingResponseRepository extends JpaRepository<CodingResponse, UUID> {

    Optional<CodingResponse> findByAssessmentIdAndQuestionId(UUID assessmentId, UUID questionId);
}
