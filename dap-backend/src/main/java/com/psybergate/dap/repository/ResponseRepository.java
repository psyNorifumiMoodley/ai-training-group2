package com.psybergate.dap.repository;

import com.psybergate.dap.domain.Response;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResponseRepository extends JpaRepository<Response, UUID> {

    Optional<Response> findByAssessmentIdAndQuestionId(UUID assessmentId, UUID questionId);

    List<Response> findByAssessmentId(UUID assessmentId);

    @Query("""
            SELECT r FROM Response r
            WHERE r.assessment.id = :assessmentId
              AND r.id NOT IN (
                SELECT c.id FROM QuestionGroupResponse g JOIN g.childResponses c
                WHERE g.assessment.id = :assessmentId
              )
            """)
    List<Response> findTopLevelByAssessmentId(@Param("assessmentId") UUID assessmentId);

    @Query("SELECT r FROM Response r JOIN FETCH r.question WHERE r.assessment.id = :assessmentId")
    List<Response> findWithQuestionByAssessmentId(@Param("assessmentId") UUID assessmentId);
}
