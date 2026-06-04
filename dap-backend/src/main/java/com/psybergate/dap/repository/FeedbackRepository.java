package com.psybergate.dap.repository;

import com.psybergate.dap.domain.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {

    Optional<Feedback> findByAssessmentIdAndQuestionId(UUID assessmentId, UUID questionId);

    List<Feedback> findByAssessmentId(UUID assessmentId);

    @Query("""
            SELECT f.question.id FROM Feedback f
            WHERE f.assessment.id = :assessmentId
              AND (f.draft IS NULL OR TRIM(f.draft) = '')
            """)
    List<UUID> findQuestionsWithEmptyFeedback(@Param("assessmentId") UUID assessmentId);

    @Query("SELECT COUNT(f) > 0 FROM Feedback f WHERE f.assessment.id = :assessmentId AND (f.draft IS NULL OR f.draft = '')")
    boolean existsByAssessmentIdAndDraftIsEmpty(@Param("assessmentId") UUID assessmentId);
}
