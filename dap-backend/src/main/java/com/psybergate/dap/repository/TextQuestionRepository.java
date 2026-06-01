package com.psybergate.dap.repository;

import com.psybergate.dap.domain.TextQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TextQuestionRepository extends JpaRepository<TextQuestion, UUID> {
}
