package com.psybergate.dap.repository;

import com.psybergate.dap.domain.McqQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface McqQuestionRepository extends JpaRepository<McqQuestion, UUID> {
}
