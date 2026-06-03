package com.psybergate.dap.repository;

import com.psybergate.dap.domain.Response;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ResponseRepository extends JpaRepository<Response, UUID> {

    List<Response> findByAssessmentId(UUID assessmentId);
}
