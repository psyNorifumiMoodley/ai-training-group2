package com.psybergate.dap.repository;

import com.psybergate.dap.domain.Candidate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CandidateRepository extends JpaRepository<Candidate, UUID> {

    boolean existsByUserEmail(String email);

    Optional<Candidate> findByUserEmail(String email);

    @Override
    @EntityGraph(attributePaths = "user")
    Page<Candidate> findAll(Pageable pageable);
}
