package com.psybergate.dap.repository;

import com.psybergate.dap.domain.CodingQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CodingQuestionRepository extends JpaRepository<CodingQuestion, UUID> {
}
