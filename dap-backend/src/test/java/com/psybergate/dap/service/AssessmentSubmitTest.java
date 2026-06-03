package com.psybergate.dap.service;

import com.psybergate.dap.config.JwtUtil;
import com.psybergate.dap.domain.*;
import com.psybergate.dap.dto.AssessmentResponse;
import com.psybergate.dap.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AssessmentSubmitTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

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
    private JwtUtil jwtUtil;
    @Autowired
    private TransactionTemplate txTemplate;

    private Candidate candidate;
    private String candidateToken;

    @BeforeEach
    void setUp() {
        txTemplate.execute(status -> {
            AppUser user = AppUser.builder()
                    .email("submit-candidate-" + UUID.randomUUID() + "@test.com")
                    .name("Submit Candidate")
                    .passwordHash("x")
                    .role(Role.CANDIDATE)
                    .build();
            appUserRepository.save(user);
            candidate = candidateRepository.save(Candidate.builder().user(user).build());
            candidateToken = jwtUtil.generateToken(user);
            return null;
        });
    }

    private Assessment inProgressAssessment(Instant startTime, int timeLimitMinutes) {
        return txTemplate.execute(status -> {
            McqQuestion q = new McqQuestion(List.of("A", "B"), List.of("A"));
            q.setCategory("Java");
            q.setQuestion("What is Java?");
            mcqQuestionRepository.save(q);

            Candidate managedCandidate = candidateRepository.findById(candidate.getId()).orElseThrow();
            Assessment a = Assessment.builder()
                    .candidate(managedCandidate)
                    .status(AssessmentStatus.IN_PROGRESS)
                    .timeLimitMinutes(timeLimitMinutes)
                    .startTime(startTime)
                    .questions(List.of(q))
                    .build();
            return assessmentRepository.save(a);
        });
    }

    private ResponseEntity<AssessmentResponse> doSubmit(UUID assessmentId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(candidateToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(
                "/api/assessments/{id}/submit",
                HttpMethod.POST,
                new HttpEntity<>(null, headers),
                AssessmentResponse.class,
                assessmentId);
    }

    @Test
    void submit_inTime_statusSubmittedAndNotAutoSubmitted() {
        Assessment assessment = inProgressAssessment(Instant.now(), 60);

        ResponseEntity<AssessmentResponse> response = doSubmit(assessment.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("SUBMITTED");

        Assessment updated = assessmentRepository.findById(assessment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AssessmentStatus.SUBMITTED);
        assertThat(updated.isAutoSubmitted()).isFalse();
    }

    @Test
    void submit_afterTimeElapsed_statusSubmittedAndAutoSubmitted() {
        Instant startedTwoMinutesAgo = Instant.now().minusSeconds(120);
        Assessment assessment = inProgressAssessment(startedTwoMinutesAgo, 1);

        ResponseEntity<AssessmentResponse> response = doSubmit(assessment.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Assessment updated = assessmentRepository.findById(assessment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AssessmentStatus.SUBMITTED);
        assertThat(updated.isAutoSubmitted()).isTrue();
    }

    @Test
    void submit_twice_returns409() {
        Assessment assessment = inProgressAssessment(Instant.now(), 60);

        doSubmit(assessment.getId());
        ResponseEntity<AssessmentResponse> second = doSubmit(assessment.getId());

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
