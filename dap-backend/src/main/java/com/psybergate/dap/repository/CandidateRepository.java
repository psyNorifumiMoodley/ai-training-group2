package com.psybergate.dap.repository;

import com.psybergate.dap.domain.AssessmentStatus;
import com.psybergate.dap.domain.Candidate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CandidateRepository extends JpaRepository<Candidate, UUID> {

    boolean existsByUserEmail(String email);

    Optional<Candidate> findByUserEmail(String email);

    @Override
    @EntityGraph(attributePaths = "user")
    Page<Candidate> findAll(Pageable pageable);

    @Query(value = """
            SELECT c FROM Candidate c
            JOIN FETCH c.user u
            WHERE (CAST(:search AS String) IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')))
            AND (:status IS NULL OR EXISTS (
                SELECT 1 FROM Assessment a WHERE a.candidate = c AND a.status = :status
            ))
            """,
            countQuery = """
            SELECT COUNT(c) FROM Candidate c
            JOIN c.user u
            WHERE (CAST(:search AS String) IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')))
            AND (:status IS NULL OR EXISTS (
                SELECT 1 FROM Assessment a WHERE a.candidate = c AND a.status = :status
            ))
            """)
    Page<Candidate> search(
            @Param("search") String search,
            @Param("status") AssessmentStatus status,
            Pageable pageable
    );
}
