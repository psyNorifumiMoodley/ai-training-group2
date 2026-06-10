package com.psybergate.dap.repository;

import com.psybergate.dap.domain.QuestionBank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QuestionBankRepository extends JpaRepository<QuestionBank, UUID> {

    Optional<QuestionBank> findByName(String name);

    boolean existsByQuestionsId(UUID questionId);
}
