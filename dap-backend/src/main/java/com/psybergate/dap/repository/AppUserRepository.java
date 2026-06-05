package com.psybergate.dap.repository;

import com.psybergate.dap.domain.AppUser;
import com.psybergate.dap.domain.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<AppUser> findAllByRole(Role role, Pageable pageable);

    @Query(value = """
            SELECT u FROM AppUser u
            WHERE u.role = 'MARKER'
            AND (CAST(:search AS String) IS NULL
                 OR LOWER(u.name) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                 OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')))
            """,
            countQuery = """
            SELECT COUNT(u) FROM AppUser u
            WHERE u.role = 'MARKER'
            AND (CAST(:search AS String) IS NULL
                 OR LOWER(u.name) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))
                 OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')))
            """)
    Page<AppUser> searchMarkers(@Param("search") String search, Pageable pageable);
}
