package com.psybergate.dap.service;

import com.psybergate.dap.config.InvitationTokenUtil;
import com.psybergate.dap.domain.*;
import com.psybergate.dap.dto.*;
import com.psybergate.dap.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AssessmentService {

    private static final Logger log = LoggerFactory.getLogger(AssessmentService.class);

    @Value("${assessment.required-mcq}")
    private int requiredMcq;

    @Value("${assessment.required-text}")
    private int requiredText;

    @Value("${assessment.required-doc}")
    private int requiredDoc;

    @Value("${assessment.required-group}")
    private int requiredGroup;

    @Value("${assessment.doc-question-limit}")
    private int docQuestionLimit;

    @Value("${app.frontend-base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    private final CandidateRepository candidateRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final McqQuestionRepository mcqQuestionRepository;
    private final TextQuestionRepository textQuestionRepository;
    private final DocQuestionRepository docQuestionRepository;
    private final GroupQuestionRepository groupQuestionRepository;
    private final InvitationTokenUtil invitationTokenUtil;
    private final EmailService emailService;

    public AssessmentService(CandidateRepository candidateRepository,
                             AssessmentRepository assessmentRepository,
                             AssessmentQuestionRepository assessmentQuestionRepository,
                             McqQuestionRepository mcqQuestionRepository,
                             TextQuestionRepository textQuestionRepository,
                             DocQuestionRepository docQuestionRepository,
                             GroupQuestionRepository groupQuestionRepository,
                             InvitationTokenUtil invitationTokenUtil,
                             EmailService emailService) {
        this.candidateRepository = candidateRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentQuestionRepository = assessmentQuestionRepository;
        this.mcqQuestionRepository = mcqQuestionRepository;
        this.textQuestionRepository = textQuestionRepository;
        this.docQuestionRepository = docQuestionRepository;
        this.groupQuestionRepository = groupQuestionRepository;
        this.invitationTokenUtil = invitationTokenUtil;
        this.emailService = emailService;
    }

    @Transactional
    public AssessmentResponse generate(AssessmentRequest request) {
        Candidate candidate = candidateRepository.findById(request.candidateId())
                .orElseThrow(() -> new NoSuchElementException("Candidate not found: " + request.candidateId()));

        Set<UUID> seenIds = new HashSet<>(fetchSeenQuestionIds(request.candidateId()));

        List<AssessmentQuestion> questions;
        if (request.questionIds() == null || request.questionIds().isEmpty()) {
            questions = selectRandomQuestions(seenIds);
        } else {
            questions = selectMarkerPickedQuestions(request.questionIds(), seenIds);
        }

        long docCount = questions.stream().filter(q -> q instanceof DocQuestion).count();
        if (docCount > docQuestionLimit) {
            throw new ValidationException(
                    "Assessment contains " + docCount + " document questions but limit is " + docQuestionLimit);
        }

        Assessment assessment = Assessment.builder()
                .candidate(candidate)
                .status(AssessmentStatus.PENDING)
                .timeLimitMinutes(request.timeLimitMinutes())
                .questions(questions)
                .build();

        Assessment saved = assessmentRepository.save(assessment);

        String token = invitationTokenUtil.generateToken(saved.getId());
        saved.setInvitationToken(token);
        saved = assessmentRepository.save(saved);

        String candidateEmail = candidate.getUser().getEmail();
        String candidateName = candidate.getUser().getName();
        String invitationLink = frontendBaseUrl + "/assessment/" + token;
        try {
            emailService.sendInvitation(candidateEmail, candidateName, invitationLink);
        } catch (Exception ex) {
            log.error("Failed to send invitation email for assessment {}: {}", saved.getId(), ex.getMessage(), ex);
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Assessment getOrThrow(UUID id) {
        return assessmentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Assessment not found: " + id));
    }

    @Transactional
    public AssessmentAccessResponse access(String token) {
        if (!invitationTokenUtil.isSignatureValid(token)) {
            throw new UnauthorizedException("Invalid invitation token");
        }

        Assessment assessment = assessmentRepository.findByInvitationToken(token)
                .orElseThrow(() -> new UnauthorizedException("Assessment not found for the provided token"));

        AssessmentStatus status = assessment.getStatus();
        if (status == AssessmentStatus.SUBMITTED || status == AssessmentStatus.MARKED) {
            throw new ConflictException("Assessment has already been submitted");
        }

        if (assessment.getStartTime() != null) {
            Instant deadline = assessment.getStartTime().plus(assessment.getTimeLimitMinutes(), ChronoUnit.MINUTES);
            if (Instant.now().isAfter(deadline)) {
                throw new UnauthorizedException("Assessment session has expired");
            }
        }

        if (status == AssessmentStatus.PENDING) {
            assessment.setStatus(AssessmentStatus.IN_PROGRESS);
            assessment.setStartTime(Instant.now());
            assessment = assessmentRepository.save(assessment);
        }

        Instant deadline = assessment.getStartTime().plus(assessment.getTimeLimitMinutes(), ChronoUnit.MINUTES);
        long remainingSeconds = ChronoUnit.SECONDS.between(Instant.now(), deadline);

        List<QuestionResponse> questionResponses = assessment.getQuestions().stream()
                .map(this::toQuestionResponse)
                .collect(Collectors.toList());

        return new AssessmentAccessResponse(assessment.getId(), questionResponses, (int) remainingSeconds);
    }

    private QuestionResponse toQuestionResponse(AssessmentQuestion q) {
        if (q instanceof McqQuestion mq) {
            return new McqQuestionResponse(mq.getId(), mq.getCategory(), mq.getQuestion(),
                    mq.getOptions(), mq.getCorrectAnswers());
        }
        if (q instanceof DocQuestion dq) {
            return new DocQuestionResponse(dq.getId(), dq.getCategory(), dq.getQuestion());
        }
        if (q instanceof GroupQuestion gq) {
            List<TextQuestionResponse> followUps = gq.getFollowUpQuestions().stream()
                    .map(fq -> new TextQuestionResponse(fq.getId(), fq.getCategory(), fq.getQuestion(), fq.getKeywords()))
                    .collect(Collectors.toList());
            return new GroupQuestionResponse(gq.getId(), gq.getCategory(), gq.getQuestion(), gq.isOrdered(), followUps);
        }
        if (q instanceof TextQuestion tq) {
            return new TextQuestionResponse(tq.getId(), tq.getCategory(), tq.getQuestion(), tq.getKeywords());
        }
        throw new UnsupportedOperationException("Unmapped question type: " + q.getClass());
    }

    private List<UUID> fetchSeenQuestionIds(UUID candidateId) {
        int currentYear = LocalDate.now().getYear();
        Instant yearStart = LocalDate.of(currentYear, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant yearEnd = LocalDate.of(currentYear + 1, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return assessmentRepository.findSeenQuestionIdsByCandidateAndYear(
                candidateId, AssessmentStatus.SUBMITTED, yearStart, yearEnd);
    }

    private List<AssessmentQuestion> selectRandomQuestions(Set<UUID> seenIds) {
        List<AssessmentQuestion> selected = new ArrayList<>();
        selected.addAll(pickRandom(availableMcq(seenIds), requiredMcq, "MCQ"));
        selected.addAll(pickRandom(availablePureText(seenIds), requiredText, "Text"));
        selected.addAll(pickRandom(availableDoc(seenIds), requiredDoc, "Document"));
        selected.addAll(pickRandom(availableGroup(seenIds), requiredGroup, "Group"));
        return selected;
    }

    private List<AssessmentQuestion> selectMarkerPickedQuestions(List<UUID> questionIds, Set<UUID> seenIds) {
        List<AssessmentQuestion> resolved = new ArrayList<>();
        for (UUID id : questionIds) {
            AssessmentQuestion q = assessmentQuestionRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("Question not found: " + id));
            resolved.add(q);
        }

        List<AssessmentQuestion> filtered = resolved.stream()
                .filter(q -> !seenIds.contains(q.getId()))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            throw new UnprocessableException("All provided questions have been seen by this candidate this year");
        }

        validateComposition(filtered);

        return filtered;
    }

    private void validateComposition(List<AssessmentQuestion> questions) {
        long mcqCount = questions.stream().filter(q -> q instanceof McqQuestion).count();
        long groupCount = questions.stream().filter(q -> q instanceof GroupQuestion).count();
        long docCount = questions.stream().filter(q -> q instanceof DocQuestion).count();
        long textCount = questions.stream().filter(q -> q instanceof TextQuestion && !(q instanceof GroupQuestion)).count();

        if (mcqCount != requiredMcq || textCount != requiredText || docCount != requiredDoc || groupCount != requiredGroup) {
            throw new ValidationException(String.format(
                    "Assessment must contain exactly %d MCQ, %d Text, %d Document, and %d Group questions " +
                    "(got %d MCQ, %d Text, %d Document, %d Group)",
                    requiredMcq, requiredText, requiredDoc, requiredGroup,
                    mcqCount, textCount, docCount, groupCount));
        }
    }

    private List<McqQuestion> availableMcq(Set<UUID> seenIds) {
        return mcqQuestionRepository.findAll().stream()
                .filter(q -> !seenIds.contains(q.getId()))
                .collect(Collectors.toList());
    }

    private List<TextQuestion> availablePureText(Set<UUID> seenIds) {
        return textQuestionRepository.findAll().stream()
                .filter(q -> !(q instanceof GroupQuestion) && !seenIds.contains(q.getId()))
                .collect(Collectors.toList());
    }

    private List<DocQuestion> availableDoc(Set<UUID> seenIds) {
        return docQuestionRepository.findAll().stream()
                .filter(q -> !seenIds.contains(q.getId()))
                .collect(Collectors.toList());
    }

    private List<GroupQuestion> availableGroup(Set<UUID> seenIds) {
        return groupQuestionRepository.findAll().stream()
                .filter(q -> !seenIds.contains(q.getId()))
                .collect(Collectors.toList());
    }

    private <T extends AssessmentQuestion> List<T> pickRandom(List<T> available, int count, String type) {
        if (available.size() < count) {
            throw new UnprocessableException(
                    "Not enough available " + type + " questions: need " + count + ", found " + available.size());
        }
        Collections.shuffle(available);
        return new ArrayList<>(available.subList(0, count));
    }

    @Transactional(readOnly = true)
    public List<UUID> getSeenQuestionIds(UUID candidateId) {
        return fetchSeenQuestionIds(candidateId);
    }

    private AssessmentResponse toResponse(Assessment assessment) {
        String invitationLink = frontendBaseUrl + "/assessment/" + assessment.getInvitationToken();
        return new AssessmentResponse(
                assessment.getId(),
                assessment.getCandidate().getId(),
                assessment.getStatus().name(),
                invitationLink,
                assessment.getTimeLimitMinutes(),
                assessment.getCreatedAt() != null ? assessment.getCreatedAt().toString() : null
        );
    }
}
