package com.psybergate.dap.repository;

import com.psybergate.dap.domain.TestCaseResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TestCaseResultRepository extends JpaRepository<TestCaseResult, UUID> {

    List<TestCaseResult> findAllByCodingResponseId(UUID codingResponseId);

    void deleteAllByCodingResponseId(UUID codingResponseId);
}
