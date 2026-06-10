package com.psybergate.dap.repository;

import com.psybergate.dap.domain.AppUser;
import com.psybergate.dap.domain.Assessment;
import com.psybergate.dap.domain.AssessmentStatus;
import com.psybergate.dap.domain.Candidate;
import com.psybergate.dap.domain.Feedback;
import com.psybergate.dap.domain.McqQuestion;
import com.psybergate.dap.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class FeedbackRepositoryTest {

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
    private AssessmentRepository assessmentRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    // ---------- helpers ----------

    private Candidate persistCandidate(String email) {
        AppUser user = AppUser.builder()
                .email(email)
                .name("Test User")
                .passwordHash("x")
                .role(Role.CANDIDATE)
                .build();
        appUserRepository.save(user);
        Candidate candidate = Candidate.builder().user(user).build();
        return candidateRepository.save(candidate);
    }

    private McqQuestion persistMcq() {
        McqQuestion q = new McqQuestion(List.of("A", "B"), List.of("A"));
        q.setQuestion("What is Java?");
        return mcqQuestionRepository.save(q);
    }

    private Assessment persistAssessment(Candidate candidate) {
        Assessment a = Assessment.builder()
                .candidate(candidate)
                .status(AssessmentStatus.SUBMITTED)
                .timeLimitMinutes(60)
                .build();
        return assessmentRepository.save(a);
    }

    private Feedback persistFeedback(Assessment assessment, McqQuestion question, String draft) {
        Feedback feedback = Feedback.builder()
                .assessment(assessment)
                .question(question)
                .draft(draft)
                .finalised(false)
                .build();
        return feedbackRepository.save(feedback);
    }

    // ---------- findByAssessmentIdAndQuestionId ----------

    @Test
    void findByAssessmentIdAndQuestionId_returnsSavedDraft() {
        Candidate candidate = persistCandidate("feedback-find@test.com");
        McqQuestion question = persistMcq();
        Assessment assessment = persistAssessment(candidate);
        persistFeedback(assessment, question, "Correct");

        Optional<Feedback> found = feedbackRepository.findByAssessmentIdAndQuestionId(
                assessment.getId(), question.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getDraft()).isEqualTo("Correct");
        assertThat(found.get().isFinalised()).isFalse();
    }

    @Test
    void findByAssessmentIdAndQuestionId_returnsEmptyForUnknownPair() {
        Optional<Feedback> found = feedbackRepository.findByAssessmentIdAndQuestionId(
                java.util.UUID.randomUUID(), java.util.UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    // ---------- findByAssessmentId ----------

    @Test
    void findByAssessmentId_returnsAllFeedbackForAssessment() {
        Candidate candidate = persistCandidate("feedback-list@test.com");
        McqQuestion question1 = persistMcq();
        McqQuestion question2 = persistMcq();
        Assessment assessment = persistAssessment(candidate);

        persistFeedback(assessment, question1, "Correct");
        persistFeedback(assessment, question2, "Incorrect — please review this topic");

        List<Feedback> results = feedbackRepository.findByAssessmentId(assessment.getId());

        assertThat(results).hasSize(2);
    }

    @Test
    void findByAssessmentId_returnsEmptyForAssessmentWithNoFeedback() {
        Candidate candidate = persistCandidate("feedback-empty@test.com");
        Assessment assessment = persistAssessment(candidate);

        List<Feedback> results = feedbackRepository.findByAssessmentId(assessment.getId());

        assertThat(results).isEmpty();
    }

    // ---------- unique constraint: same assessment + question ----------

    @Test
    void savingFeedbackTwiceForSameAssessmentAndQuestion_throwsDataIntegrityViolationException() {
        Candidate candidate = persistCandidate("feedback-unique@test.com");
        McqQuestion question = persistMcq();
        Assessment assessment = persistAssessment(candidate);

        persistFeedback(assessment, question, "First draft");

        // Force flush to ensure the first insert is committed before the second attempt
        feedbackRepository.flush();

        assertThatThrownBy(() -> {
            persistFeedback(assessment, question, "Second draft");
            feedbackRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------- existsByAssessmentIdAndDraftIsEmpty ----------

    @Test
    void existsByAssessmentIdAndDraftIsEmpty_trueWhenEmptyDraftExists() {
        Candidate candidate = persistCandidate("feedback-empty-draft@test.com");
        McqQuestion question = persistMcq();
        Assessment assessment = persistAssessment(candidate);
        persistFeedback(assessment, question, "");

        boolean exists = feedbackRepository.existsByAssessmentIdAndDraftIsEmpty(assessment.getId());

        assertThat(exists).isTrue();
    }

    @Test
    void existsByAssessmentIdAndDraftIsEmpty_falseWhenAllDraftsNonEmpty() {
        Candidate candidate = persistCandidate("feedback-nonempty-draft@test.com");
        McqQuestion question = persistMcq();
        Assessment assessment = persistAssessment(candidate);
        persistFeedback(assessment, question, "Some feedback");

        boolean exists = feedbackRepository.existsByAssessmentIdAndDraftIsEmpty(assessment.getId());

        assertThat(exists).isFalse();
    }
}
