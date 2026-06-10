package com.psybergate.dap.service;

import com.psybergate.dap.config.JwtUtil;
import com.psybergate.dap.domain.*;
import com.psybergate.dap.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class FinaliseTest {

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
    private TextQuestionRepository textQuestionRepository;
    @Autowired
    private McqQuestionRepository mcqQuestionRepository;
    @Autowired
    private FeedbackRepository feedbackRepository;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private TransactionTemplate txTemplate;

    private String markerToken;
    private Candidate candidate;

    @BeforeEach
    void setUp() {
        txTemplate.execute(status -> {
            AppUser markerUser = AppUser.builder()
                    .email("marker-" + UUID.randomUUID() + "@test.com")
                    .name("Test Marker")
                    .passwordHash("x")
                    .role(Role.MARKER)
                    .build();
            appUserRepository.save(markerUser);
            markerToken = jwtUtil.generateToken(markerUser);

            AppUser candidateUser = AppUser.builder()
                    .email("candidate-" + UUID.randomUUID() + "@test.com")
                    .name("Test Candidate")
                    .passwordHash("x")
                    .role(Role.CANDIDATE)
                    .build();
            appUserRepository.save(candidateUser);
            candidate = candidateRepository.save(Candidate.builder().user(candidateUser).build());
            return null;
        });
    }

    private Assessment submittedAssessment(List<AssessmentQuestion> questions) {
        return txTemplate.execute(status -> {
            Candidate managedCandidate = candidateRepository.findById(candidate.getId()).orElseThrow();
            Assessment a = Assessment.builder()
                    .candidate(managedCandidate)
                    .status(AssessmentStatus.SUBMITTED)
                    .timeLimitMinutes(60)
                    .questions(questions)
                    .build();
            return assessmentRepository.save(a);
        });
    }

    private HttpHeaders markerHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(markerToken);
        return headers;
    }

    private ResponseEntity<Void> doFinalise(UUID assessmentId) {
        return restTemplate.exchange(
                "/api/assessments/{id}/finalise",
                HttpMethod.POST,
                new HttpEntity<>(null, markerHeaders()),
                Void.class,
                assessmentId);
    }

    @Test
    void finalise_allTextFeedbackFilled_statusBecomesMarked() {
        TextQuestion question = txTemplate.execute(status -> {
            TextQuestion q = new TextQuestion(List.of("jvm"), 1);
            q.setQuestion("What is a JVM?");
            return textQuestionRepository.save(q);
        });

        Assessment assessment = submittedAssessment(List.of(question));

        txTemplate.execute(status -> {
            Assessment a = assessmentRepository.findById(assessment.getId()).orElseThrow();
            TextQuestion q = textQuestionRepository.findById(question.getId()).orElseThrow();
            feedbackRepository.save(Feedback.builder()
                    .assessment(a)
                    .question(q)
                    .draft("Good explanation of JVM internals.")
                    .finalised(false)
                    .build());
            return null;
        });

        ResponseEntity<Void> response = doFinalise(assessment.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Assessment updated = assessmentRepository.findById(assessment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AssessmentStatus.MARKED);

        List<Feedback> feedbacks = feedbackRepository.findByAssessmentId(assessment.getId());
        assertThat(feedbacks).allMatch(Feedback::isFinalised);
    }

    @Test
    void finalise_textFeedbackEmpty_returns400WithQuestionId() {
        TextQuestion question = txTemplate.execute(status -> {
            TextQuestion q = new TextQuestion(List.of("jvm"), 1);
            q.setQuestion("What is a JVM?");
            return textQuestionRepository.save(q);
        });

        Assessment assessment = submittedAssessment(List.of(question));

        txTemplate.execute(status -> {
            Assessment a = assessmentRepository.findById(assessment.getId()).orElseThrow();
            TextQuestion q = textQuestionRepository.findById(question.getId()).orElseThrow();
            feedbackRepository.save(Feedback.builder()
                    .assessment(a)
                    .question(q)
                    .draft("")
                    .finalised(false)
                    .build());
            return null;
        });

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/assessments/{id}/finalise",
                HttpMethod.POST,
                new HttpEntity<>(null, markerHeaders()),
                String.class,
                assessment.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains(question.getId().toString());
    }

    @Test
    void finalise_alreadyMarked_returns409() {
        Assessment assessment = txTemplate.execute(status -> {
            Candidate managedCandidate = candidateRepository.findById(candidate.getId()).orElseThrow();
            return assessmentRepository.save(Assessment.builder()
                    .candidate(managedCandidate)
                    .status(AssessmentStatus.MARKED)
                    .timeLimitMinutes(60)
                    .build());
        });

        ResponseEntity<Void> response = doFinalise(assessment.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void finalise_pendingAssessment_returns409() {
        Assessment assessment = txTemplate.execute(status -> {
            Candidate managedCandidate = candidateRepository.findById(candidate.getId()).orElseThrow();
            return assessmentRepository.save(Assessment.builder()
                    .candidate(managedCandidate)
                    .status(AssessmentStatus.PENDING)
                    .timeLimitMinutes(60)
                    .build());
        });

        ResponseEntity<Void> response = doFinalise(assessment.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void finalise_inProgressAssessment_returns409() {
        Assessment assessment = txTemplate.execute(status -> {
            Candidate managedCandidate = candidateRepository.findById(candidate.getId()).orElseThrow();
            return assessmentRepository.save(Assessment.builder()
                    .candidate(managedCandidate)
                    .status(AssessmentStatus.IN_PROGRESS)
                    .timeLimitMinutes(60)
                    .build());
        });

        ResponseEntity<Void> response = doFinalise(assessment.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void finalise_mcqOnlyAssessment_finalisesWithoutError() {
        McqQuestion question = txTemplate.execute(status -> {
            McqQuestion q = new McqQuestion(List.of("A", "B"), List.of("A"));
            q.setQuestion("Which is correct?");
            return mcqQuestionRepository.save(q);
        });

        Assessment assessment = submittedAssessment(List.of(question));

        ResponseEntity<Void> response = doFinalise(assessment.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assessment updated = assessmentRepository.findById(assessment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AssessmentStatus.MARKED);
    }

    @Test
    void finalise_mcqWithNoFeedbackRecord_andTextWithFeedback_succeeds() {
        McqQuestion mcq = txTemplate.execute(status -> {
            McqQuestion q = new McqQuestion(List.of("A", "B"), List.of("A"));
            q.setQuestion("Which is correct?");
            return mcqQuestionRepository.save(q);
        });

        TextQuestion text = txTemplate.execute(status -> {
            TextQuestion q = new TextQuestion(List.of("jvm"), 1);
            q.setQuestion("What is a JVM?");
            return textQuestionRepository.save(q);
        });

        Assessment assessment = submittedAssessment(List.of(mcq, text));

        txTemplate.execute(status -> {
            Assessment a = assessmentRepository.findById(assessment.getId()).orElseThrow();
            TextQuestion q = textQuestionRepository.findById(text.getId()).orElseThrow();
            feedbackRepository.save(Feedback.builder()
                    .assessment(a)
                    .question(q)
                    .draft("Good explanation.")
                    .finalised(false)
                    .build());
            return null;
        });

        ResponseEntity<Void> response = doFinalise(assessment.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assessment updated = assessmentRepository.findById(assessment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AssessmentStatus.MARKED);
    }

    @Test
    void finalise_withOverallFeedbackBody_returns200() {
        TextQuestion question = txTemplate.execute(status -> {
            TextQuestion q = new TextQuestion(List.of("jvm"), 1);
            q.setQuestion("What is a JVM?");
            return textQuestionRepository.save(q);
        });

        Assessment assessment = submittedAssessment(List.of(question));

        txTemplate.execute(status -> {
            Assessment a = assessmentRepository.findById(assessment.getId()).orElseThrow();
            TextQuestion q = textQuestionRepository.findById(question.getId()).orElseThrow();
            feedbackRepository.save(Feedback.builder()
                    .assessment(a)
                    .question(q)
                    .draft("Well done.")
                    .finalised(false)
                    .build());
            return null;
        });

        HttpHeaders headers = markerHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/assessments/{id}/finalise",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("overallFeedback", "Strong performance overall."), headers),
                Void.class,
                assessment.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
