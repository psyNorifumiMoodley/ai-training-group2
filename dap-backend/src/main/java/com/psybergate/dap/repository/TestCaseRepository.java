package com.psybergate.dap.repository;

import com.psybergate.dap.domain.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TestCaseRepository extends JpaRepository<TestCase, UUID> {

    List<TestCase> findByCodingQuestionIdOrderByOrdinalAsc(UUID codingQuestionId);

    int countByCodingQuestionId(UUID codingQuestionId);
}
