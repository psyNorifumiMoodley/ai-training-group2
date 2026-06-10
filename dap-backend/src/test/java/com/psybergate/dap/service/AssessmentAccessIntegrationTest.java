package com.psybergate.dap.service;

import com.psybergate.dap.config.InvitationTokenUtil;
import com.psybergate.dap.domain.*;
import com.psybergate.dap.dto.AssessmentAccessResponse;
import com.psybergate.dap.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AssessmentAccessIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private McqQuestionRepository mcqQuestionRepository;

    @Autowired
    private InvitationTokenUtil invitationTokenUtil;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private String accessUrl(String token) {
        return "http://localhost:" + port + "/api/assessments/access/" + token;
    }

    /**
     * Creates and persists all entities needed for a single test inside one transaction,
     * so that @MapsId on Candidate can resolve the AppUser as a managed entity.
     * Uses a unique email per call to avoid unique-constraint conflicts between tests.
     */
    private Assessment createAssessment(AssessmentStatus status, Instant startTime) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        UUID[] assessmentIdHolder = new UUID[1];

        tx.execute(txStatus -> {
            String uniqueEmail = "candidate-" + UUID.randomUUID() + "@test.com";

            AppUser user = AppUser.builder()
                    .email(uniqueEmail)
                    .name("Test Candidate")
                    .passwordHash("x")
                    .role(Role.CANDIDATE)
                    .build();
            appUserRepository.save(user);

            Candidate candidate = Candidate.builder().user(user).build();
            candidateRepository.save(candidate);

            McqQuestion mcq = new McqQuestion(List.of("A", "B"), List.of("A"));
            mcq.setQuestion("What is Java?");
            mcqQuestionRepository.save(mcq);

            Assessment assessment = Assessment.builder()
                    .candidate(candidate)
                    .status(status)
                    .timeLimitMinutes(60)
                    .questions(new ArrayList<>(List.of(mcq)))
                    .build();
            assessment.setStartTime(startTime);

            Assessment saved = assessmentRepository.save(assessment);
            String token = invitationTokenUtil.generateToken(saved.getId());
            saved.setInvitationToken(token);
            Assessment withToken = assessmentRepository.save(saved);

            assessmentIdHolder[0] = withToken.getId();
            return null;
        });

        return assessmentRepository.findById(assessmentIdHolder[0]).orElseThrow();
    }

    // --- PENDING → 200, transitions to IN_PROGRESS ---

    @Test
    void access_pendingAssessment_returns200AndTransitionsToInProgress() {
        Assessment assessment = createAssessment(AssessmentStatus.PENDING, null);
        String token = assessment.getInvitationToken();

        ResponseEntity<AssessmentAccessResponse> response =
                restTemplate.getForEntity(accessUrl(token), AssessmentAccessResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().assessmentId()).isEqualTo(assessment.getId());
        assertThat(response.getBody().remainingSeconds()).isGreaterThan(0);

        // Verify DB state: status should now be IN_PROGRESS and startTime set
        Assessment updated = assessmentRepository.findById(assessment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AssessmentStatus.IN_PROGRESS);
        assertThat(updated.getStartTime()).isNotNull();
    }

    // --- Already IN_PROGRESS → 200, startTime unchanged ---

    @Test
    void access_inProgressAssessment_returns200AndStartTimeUnchanged() {
        Instant originalStartTime = Instant.now().minus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);
        Assessment assessment = createAssessment(AssessmentStatus.IN_PROGRESS, originalStartTime);
        String token = assessment.getInvitationToken();

        ResponseEntity<AssessmentAccessResponse> response =
                restTemplate.getForEntity(accessUrl(token), AssessmentAccessResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().remainingSeconds()).isGreaterThan(0);

        // startTime must not have changed
        Assessment reloaded = assessmentRepository.findById(assessment.getId()).orElseThrow();
        assertThat(reloaded.getStartTime().truncatedTo(ChronoUnit.SECONDS))
                .isEqualTo(originalStartTime);
        assertThat(reloaded.getStatus()).isEqualTo(AssessmentStatus.IN_PROGRESS);
    }

    // --- SUBMITTED → 409 ---

    @Test
    void access_submittedAssessment_returns409() {
        Assessment assessment = createAssessment(AssessmentStatus.SUBMITTED,
                Instant.now().minus(30, ChronoUnit.MINUTES));
        String token = assessment.getInvitationToken();

        ResponseEntity<String> response = restTemplate.getForEntity(accessUrl(token), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // --- Tampered token → 401 ---

    @Test
    void access_tamperedToken_returns401() {
        Assessment assessment = createAssessment(AssessmentStatus.PENDING, null);
        String validToken = assessment.getInvitationToken();
        // Corrupt the signature portion
        String tamperedToken = validToken.substring(0, validToken.length() - 5) + "XXXXX";

        ResponseEntity<String> response = restTemplate.getForEntity(accessUrl(tamperedToken), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- Expired session → 401 ---

    @Test
    void access_expiredSession_returns401() {
        // timeLimitMinutes=60, started 90 minutes ago → session is long expired
        Instant expiredStart = Instant.now().minus(90, ChronoUnit.MINUTES);
        Assessment assessment = createAssessment(AssessmentStatus.IN_PROGRESS, expiredStart);
        String token = assessment.getInvitationToken();

        ResponseEntity<String> response = restTemplate.getForEntity(accessUrl(token), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
