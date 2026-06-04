package com.psybergate.dap.repository;

import com.psybergate.dap.domain.Assessment;
import com.psybergate.dap.domain.AssessmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {

    @Query("""
            SELECT DISTINCT aq.id FROM Assessment a
            JOIN a.questions aq
            WHERE a.candidate.id = :candidateId
              AND a.status = :status
              AND a.createdAt >= :yearStart
              AND a.createdAt < :yearEnd
            """)
    List<UUID> findSeenQuestionIdsByCandidateAndYear(
            @Param("candidateId") UUID candidateId,
            @Param("status") AssessmentStatus status,
            @Param("yearStart") Instant yearStart,
            @Param("yearEnd") Instant yearEnd
    );

    @EntityGraph(attributePaths = "questions")
    Optional<Assessment> findByInvitationToken(String token);

    List<Assessment> findByCandidateId(UUID candidateId);

    @EntityGraph(attributePaths = {"candidate", "candidate.user", "questions"})
    Page<Assessment> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"candidate", "candidate.user", "questions"})
    Page<Assessment> findByStatus(AssessmentStatus status, Pageable pageable);
}
