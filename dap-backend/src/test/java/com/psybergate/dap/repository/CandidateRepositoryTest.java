package com.psybergate.dap.repository;

import com.psybergate.dap.domain.AppUser;
import com.psybergate.dap.domain.Candidate;
import com.psybergate.dap.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class CandidateRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void save_persistsBothAppUserAndCandidateRows() {
        AppUser user = AppUser.builder()
                .email("jane@example.com")
                .name("Jane Doe")
                .role(Role.CANDIDATE)
                .passwordHash("$2a$irrelevant")
                .build();
        appUserRepository.save(user);

        Candidate candidate = Candidate.builder().user(user).build();
        candidateRepository.save(candidate);

        assertThat(appUserRepository.findByEmail("jane@example.com")).isPresent();
        assertThat(candidateRepository.findByUserEmail("jane@example.com")).isPresent();
    }

    @Test
    void existsByUserEmail_returnsTrueForExistingEmail() {
        AppUser user = AppUser.builder()
                .email("existing@example.com")
                .name("Existing User")
                .role(Role.CANDIDATE)
                .passwordHash("$2a$irrelevant")
                .build();
        appUserRepository.save(user);
        candidateRepository.save(Candidate.builder().user(user).build());

        assertThat(candidateRepository.existsByUserEmail("existing@example.com")).isTrue();
        assertThat(candidateRepository.existsByUserEmail("notfound@example.com")).isFalse();
    }

    @Test
    void existsByUserEmail_returnsFalseForUnknownEmail() {
        assertThat(candidateRepository.existsByUserEmail("ghost@example.com")).isFalse();
    }
}
