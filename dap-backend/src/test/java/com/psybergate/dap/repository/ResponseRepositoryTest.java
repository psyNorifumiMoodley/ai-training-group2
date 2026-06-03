package com.psybergate.dap.repository;

import com.psybergate.dap.domain.AppUser;
import com.psybergate.dap.domain.Assessment;
import com.psybergate.dap.domain.AssessmentStatus;
import com.psybergate.dap.domain.Candidate;
import com.psybergate.dap.domain.McqQuestion;
import com.psybergate.dap.domain.McqResponse;
import com.psybergate.dap.domain.Response;
import com.psybergate.dap.domain.Role;
import com.psybergate.dap.domain.TextQuestion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ResponseRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private CandidateRepository candidateRepository;
    @Autowired
    private McqQuestionRepository mcqQuestionRepository;
    @Autowired
    private TextQuestionRepository textQuestionRepository;
    @Autowired
    private AssessmentRepository assessmentRepository;
    @Autowired
    private ResponseRepository responseRepository;

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

    private TextQuestion persistText() {
        TextQuestion q = new TextQuestion(null);
        q.setCategory("Java");
        q.setQuestion("Explain OOP.");
        return textQuestionRepository.save(q);
    }

    private Assessment persistAssessment(Candidate candidate) {
        Assessment a = Assessment.builder()
                .candidate(candidate)
                .status(AssessmentStatus.IN_PROGRESS)
                .timeLimitMinutes(60)
                .build();
        return assessmentRepository.save(a);
    }

    // --- findByAssessmentIdAndQuestionId returns correct record for MCQ type ---

    @Test
    void findByAssessmentIdAndQuestionId_returnsMcqResponseForCorrectPair() {
        Candidate candidate = persistCandidate("mcq-resp@test.com");
        McqQuestion question = persistMcq();
        Assessment assessment = persistAssessment(candidate);

        McqResponse response = McqResponse.builder()
                .selectedAnswers(List.of("A"))
                .build();
        response.setAssessment(assessment);
        response.setQuestion(question);
        responseRepository.save(response);

        Optional<Response> found = responseRepository.findByAssessmentIdAndQuestionId(
                assessment.getId(), question.getId());

        assertThat(found).isPresent();
        assertThat(found.get()).isInstanceOf(McqResponse.class);
        McqResponse mcq = (McqResponse) found.get();
        assertThat(mcq.getSelectedAnswers()).containsExactly("A");
        assertThat(mcq.getAssessment().getId()).isEqualTo(assessment.getId());
        assertThat(mcq.getQuestion().getId()).isEqualTo(question.getId());
    }

    // --- findByAssessmentIdAndQuestionId returns empty Optional for unknown pair ---

    @Test
    void findByAssessmentIdAndQuestionId_returnsEmptyForUnknownPair() {
        Optional<Response> found = responseRepository.findByAssessmentIdAndQuestionId(
                UUID.randomUUID(), UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    // --- findByAssessmentIdAndQuestionId does not return response for different assessment ---

    @Test
    void findByAssessmentIdAndQuestionId_doesNotReturnResponseFromDifferentAssessment() {
        Candidate candidate = persistCandidate("diff-assessment@test.com");
        McqQuestion question = persistMcq();
        Assessment assessmentA = persistAssessment(candidate);
        Assessment assessmentB = persistAssessment(candidate);

        McqResponse response = McqResponse.builder()
                .selectedAnswers(List.of("B"))
                .build();
        response.setAssessment(assessmentA);
        response.setQuestion(question);
        responseRepository.save(response);

        Optional<Response> found = responseRepository.findByAssessmentIdAndQuestionId(
                assessmentB.getId(), question.getId());

        assertThat(found).isEmpty();
    }

    // --- findByAssessmentId returns all responses for the assessment ---

    @Test
    void findByAssessmentId_returnsAllResponsesForAssessment() {
        Candidate candidate = persistCandidate("all-responses@test.com");
        McqQuestion mcq1 = persistMcq();
        McqQuestion mcq2 = persistMcq();
        Assessment assessment = persistAssessment(candidate);

        McqResponse r1 = McqResponse.builder().selectedAnswers(List.of("A")).build();
        r1.setAssessment(assessment);
        r1.setQuestion(mcq1);
        responseRepository.save(r1);

        McqResponse r2 = McqResponse.builder().selectedAnswers(List.of("B")).build();
        r2.setAssessment(assessment);
        r2.setQuestion(mcq2);
        responseRepository.save(r2);

        List<Response> responses = responseRepository.findByAssessmentId(assessment.getId());

        assertThat(responses).hasSize(2);
    }
}
