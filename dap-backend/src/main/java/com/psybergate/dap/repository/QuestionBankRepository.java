package com.psybergate.dap.repository;

import com.psybergate.dap.domain.QuestionBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface QuestionBankRepository extends JpaRepository<QuestionBank, UUID> {

    Optional<QuestionBank> findByName(String name);

    boolean existsByQuestionsId(UUID questionId);

    @Query(value = "SELECT COUNT(*) FROM question_question_bank WHERE question_bank_id = :bankId", nativeQuery = true)
    long countQuestionsByBankId(@Param("bankId") UUID bankId);
}
