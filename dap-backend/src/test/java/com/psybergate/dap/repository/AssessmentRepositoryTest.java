package com.psybergate.dap.repository;

import com.psybergate.dap.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class AssessmentRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private CandidateRepository candidateRepository;
    @Autowired
    private McqQuestionRepository mcqQuestionRepository;
    @Autowired
    private AssessmentRepository assessmentRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Candidate persistCandidate(String email) {
        AppUser user = AppUser.builder()
                .email(email).name("Test User").passwordHash("x").role(Role.CANDIDATE).build();
        appUserRepository.save(user);
        Candidate candidate = Candidate.builder().user(user).build();
        return candidateRepository.save(candidate);
    }

    private McqQuestion persistMcq() {
        McqQuestion q = new McqQuestion(List.of("A", "B"), List.of("A"));
        q.setCategory("Java");
        q.setQuestion("What is Java?");
        return mcqQuestionRepository.save(q);
    }

    private Assessment persistAssessment(Candidate candidate, AssessmentStatus status,
                                         List<AssessmentQuestion> questions, Instant createdAt) {
        Assessment a = Assessment.builder()
                .candidate(candidate)
                .status(status)
                .timeLimitMinutes(60)
                .questions(questions)
                .build();
        Assessment saved = assessmentRepository.save(a);
        if (createdAt != null) {
            // createdAt is updatable=false; flush first so the row exists, then override via JDBC
            entityManager.flush();
            jdbcTemplate.update("UPDATE assessment SET created_at = ? WHERE id = ?",
                    java.sql.Timestamp.from(createdAt), saved.getId());
            entityManager.clear();
        }
        return saved;
    }

    private Instant startOfYear(int year) {
        return LocalDate.of(year, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    @Test
    void findSeenQuestionIds_returnsQuestionIdsFromSubmittedAssessmentThisYear() {
        Candidate candidate = persistCandidate("seen@test.com");
        McqQuestion q = persistMcq();
        persistAssessment(candidate, AssessmentStatus.SUBMITTED, List.of(q), null);

        int year = LocalDate.now().getYear();
        List<UUID> seen = assessmentRepository.findSeenQuestionIdsByCandidateAndYear(
                candidate.getId(), AssessmentStatus.SUBMITTED, startOfYear(year), startOfYear(year + 1));

        assertThat(seen).containsExactly(q.getId());
    }

    @Test
    void findSeenQuestionIds_excludesPendingAssessments() {
        Candidate candidate = persistCandidate("pending@test.com");
        McqQuestion q = persistMcq();
        persistAssessment(candidate, AssessmentStatus.PENDING, List.of(q), null);

        int year = LocalDate.now().getYear();
        List<UUID> seen = assessmentRepository.findSeenQuestionIdsByCandidateAndYear(
                candidate.getId(), AssessmentStatus.SUBMITTED, startOfYear(year), startOfYear(year + 1));

        assertThat(seen).isEmpty();
    }

    @Test
    void findSeenQuestionIds_excludesPriorYearAssessments() {
        Candidate candidate = persistCandidate("priorYear@test.com");
        McqQuestion q = persistMcq();
        // Use prior year's start date as createdAt
        Instant lastYear = startOfYear(LocalDate.now().getYear() - 1);
        persistAssessment(candidate, AssessmentStatus.SUBMITTED, List.of(q), lastYear);

        int year = LocalDate.now().getYear();
        List<UUID> seen = assessmentRepository.findSeenQuestionIdsByCandidateAndYear(
                candidate.getId(), AssessmentStatus.SUBMITTED, startOfYear(year), startOfYear(year + 1));

        assertThat(seen).isEmpty();
    }

    @Test
    void findSeenQuestionIds_excludesOtherCandidate() {
        Candidate candidateA = persistCandidate("a@test.com");
        Candidate candidateB = persistCandidate("b@test.com");
        McqQuestion q = persistMcq();
        persistAssessment(candidateA, AssessmentStatus.SUBMITTED, List.of(q), null);

        int year = LocalDate.now().getYear();
        List<UUID> seen = assessmentRepository.findSeenQuestionIdsByCandidateAndYear(
                candidateB.getId(), AssessmentStatus.SUBMITTED, startOfYear(year), startOfYear(year + 1));

        assertThat(seen).isEmpty();
    }

    @Test
    void findSeenQuestionIds_returnsDeduplicated() {
        Candidate candidate = persistCandidate("dedup@test.com");
        McqQuestion q = persistMcq();
        // Two submitted assessments with the same question
        persistAssessment(candidate, AssessmentStatus.SUBMITTED, List.of(q), null);
        persistAssessment(candidate, AssessmentStatus.SUBMITTED, List.of(q), null);

        int year = LocalDate.now().getYear();
        List<UUID> seen = assessmentRepository.findSeenQuestionIdsByCandidateAndYear(
                candidate.getId(), AssessmentStatus.SUBMITTED, startOfYear(year), startOfYear(year + 1));

        assertThat(seen).containsExactlyInAnyOrder(q.getId());
    }
}
