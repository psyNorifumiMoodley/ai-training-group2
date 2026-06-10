package com.psybergate.dap.repository;

import com.psybergate.dap.domain.AssessmentQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, UUID> {

    @EntityGraph(attributePaths = {"questionBanks"})
    Page<AssessmentQuestion> findAllByQuestionBanks_Id(UUID bankId, Pageable pageable);
}
