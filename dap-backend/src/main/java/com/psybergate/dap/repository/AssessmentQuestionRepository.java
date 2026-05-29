package com.psybergate.dap.repository;

import com.psybergate.dap.domain.AssessmentQuestion;
import com.psybergate.dap.domain.TextQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, UUID> {

    Page<AssessmentQuestion> findByCategory(String category, Pageable pageable);

    @Query("SELECT q FROM TextQuestion q WHERE q.id IN :ids")
    List<TextQuestion> findTextQuestionsByIds(@Param("ids") List<UUID> ids);
}
