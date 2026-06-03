package com.psybergate.dap.repository;

import com.psybergate.dap.domain.Response;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResponseRepository extends JpaRepository<Response, UUID> {

    Optional<Response> findByAssessmentIdAndQuestionId(UUID assessmentId, UUID questionId);

    List<Response> findByAssessmentId(UUID assessmentId);
}
