package com.psybergate.dap.repository;

import com.psybergate.dap.domain.McqPlusQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface McqPlusQuestionRepository extends JpaRepository<McqPlusQuestion, UUID> {
}
