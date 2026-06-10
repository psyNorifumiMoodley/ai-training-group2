package com.psybergate.dap.service;

import com.psybergate.dap.config.InvitationTokenUtil;
import com.psybergate.dap.config.JwtUtil;
import com.psybergate.dap.domain.*;
import com.psybergate.dap.dto.*;
import com.psybergate.dap.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
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
    private final FeedbackRepository feedbackRepository;
    private final InvitationTokenUtil invitationTokenUtil;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final ResponseService responseService;


    public AssessmentService(CandidateRepository candidateRepository,
                             AssessmentRepository assessmentRepository,
                             AssessmentQuestionRepository assessmentQuestionRepository,
                             McqQuestionRepository mcqQuestionRepository,
                             TextQuestionRepository textQuestionRepository,
                             DocQuestionRepository docQuestionRepository,
                             GroupQuestionRepository groupQuestionRepository,
                             InvitationTokenUtil invitationTokenUtil,
                             JwtUtil jwtUtil,
                             EmailService emailService,
                             ResponseService responseService,
                             FeedbackRepository feedbackRepository) {
        this.candidateRepository = candidateRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentQuestionRepository = assessmentQuestionRepository;
        this.mcqQuestionRepository = mcqQuestionRepository;
        this.textQuestionRepository = textQuestionRepository;
        this.docQuestionRepository = docQuestionRepository;
        this.groupQuestionRepository = groupQuestionRepository;
        this.invitationTokenUtil = invitationTokenUtil;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
        this.responseService = responseService;
        this.feedbackRepository = feedbackRepository;
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
        String invitationLink = frontendBaseUrl + "/assessment/access/" + token;
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

        long remainingSeconds;
        if (status == AssessmentStatus.PENDING || assessment.getStartTime() == null) {
            remainingSeconds = (long) assessment.getTimeLimitMinutes() * 60;
        } else {
            Instant deadline = assessment.getStartTime().plus(assessment.getTimeLimitMinutes(), ChronoUnit.MINUTES);
            remainingSeconds = ChronoUnit.SECONDS.between(Instant.now(), deadline);
        }

        List<QuestionResponse> questionResponses = assessment.getQuestions().stream()
                .map(this::toQuestionResponse)
                .collect(Collectors.toList());

        String candidateToken = jwtUtil.generateToken(assessment.getCandidate().getUser());
        boolean alreadyStarted = status == AssessmentStatus.IN_PROGRESS;
        return new AssessmentAccessResponse(assessment.getId(), questionResponses, (int) remainingSeconds, candidateToken, alreadyStarted);
    }

    @Transactional
    public void startAssessment(UUID assessmentId, UUID requestingUserId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new NoSuchElementException("Assessment not found: " + assessmentId));

        if (!assessment.getCandidate().getId().equals(requestingUserId)) {
            throw new AccessDeniedException("Forbidden: you are not the assigned candidate");
        }

        AssessmentStatus status = assessment.getStatus();
        if (status == AssessmentStatus.SUBMITTED || status == AssessmentStatus.MARKED) {
            throw new ConflictException("Assessment has already been submitted");
        }

        if (status == AssessmentStatus.PENDING) {
            assessment.setStatus(AssessmentStatus.IN_PROGRESS);
            assessment.setStartTime(Instant.now());
            assessmentRepository.save(assessment);
        }
    }

    private QuestionResponse toQuestionResponse(AssessmentQuestion q) {
        if (q instanceof McqQuestion mq) {
            return new McqQuestionResponse(mq.getId(), List.of(), mq.getQuestion(),
                    mq.getOptions(), mq.getCorrectAnswers(), mq.getCorrectAnswers().size() > 1);
        }
        if (q instanceof DocQuestion dq) {
            return new DocQuestionResponse(dq.getId(), List.of(), dq.getQuestion(), 0);
        }
        if (q instanceof GroupQuestion gq) {
            return new GroupQuestionResponse(gq.getId(), List.of(), gq.getQuestion(), gq.isOrdered(), List.of(), 0);
        }
        if (q instanceof TextQuestion tq) {
            return new TextQuestionResponse(tq.getId(), List.of(), tq.getQuestion(), tq.getKeywords(), 0);
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

        List<AssessmentQuestion> picked = resolved.stream()
                .filter(q -> !seenIds.contains(q.getId()))
                .collect(Collectors.toList());

        List<McqQuestion>   pickedMcq   = picked.stream().filter(McqQuestion.class::isInstance).map(McqQuestion.class::cast).collect(Collectors.toList());
        List<TextQuestion>  pickedText  = picked.stream().filter(q -> q.getClass() == TextQuestion.class).map(TextQuestion.class::cast).collect(Collectors.toList());
        List<DocQuestion>   pickedDoc   = picked.stream().filter(DocQuestion.class::isInstance).map(DocQuestion.class::cast).collect(Collectors.toList());
        List<GroupQuestion> pickedGroup = picked.stream().filter(GroupQuestion.class::isInstance).map(GroupQuestion.class::cast).collect(Collectors.toList());

        validateNoTypeExceedsLimit(pickedMcq.size(), pickedText.size(), pickedDoc.size(), pickedGroup.size());

        Set<UUID> pickedIds = picked.stream().map(AssessmentQuestion::getId).collect(Collectors.toSet());
        List<AssessmentQuestion> result = new ArrayList<>(picked);

        if (pickedMcq.size() < requiredMcq) {
            List<McqQuestion> available = availableMcq(seenIds).stream().filter(q -> !pickedIds.contains(q.getId())).collect(Collectors.toList());
            result.addAll(pickRandom(available, requiredMcq - pickedMcq.size(), "MCQ"));
        }
        if (pickedText.size() < requiredText) {
            List<TextQuestion> available = availablePureText(seenIds).stream().filter(q -> !pickedIds.contains(q.getId())).collect(Collectors.toList());
            result.addAll(pickRandom(available, requiredText - pickedText.size(), "Text"));
        }
        if (pickedDoc.size() < requiredDoc) {
            List<DocQuestion> available = availableDoc(seenIds).stream().filter(q -> !pickedIds.contains(q.getId())).collect(Collectors.toList());
            result.addAll(pickRandom(available, requiredDoc - pickedDoc.size(), "Document"));
        }
        if (pickedGroup.size() < requiredGroup) {
            List<GroupQuestion> available = availableGroup(seenIds).stream().filter(q -> !pickedIds.contains(q.getId())).collect(Collectors.toList());
            result.addAll(pickRandom(available, requiredGroup - pickedGroup.size(), "Group"));
        }

        return result;
    }

    private void validateNoTypeExceedsLimit(int mcq, int text, int doc, int group) {
        List<String> violations = new ArrayList<>();
        if (mcq   > requiredMcq)   violations.add(String.format("MCQ: max %d, selected %d",      requiredMcq,   mcq));
        if (text  > requiredText)  violations.add(String.format("Text: max %d, selected %d",     requiredText,  text));
        if (doc   > requiredDoc)   violations.add(String.format("Document: max %d, selected %d", requiredDoc,   doc));
        if (group > requiredGroup) violations.add(String.format("Group: max %d, selected %d",    requiredGroup, group));
        if (!violations.isEmpty()) {
            throw new ValidationException("Too many questions selected — " + String.join(", ", violations));
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

    @Transactional
    public void remind(UUID assessmentId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new NoSuchElementException("Assessment not found: " + assessmentId));

        if (assessment.getStatus() != AssessmentStatus.PENDING) {
            throw new ConflictException("Reminders can only be sent for PENDING assessments");
        }

        if (assessment.getInvitationToken() == null) {
            throw new IllegalStateException("Assessment " + assessmentId + " has no invitation token");
        }

        String candidateEmail = assessment.getCandidate().getUser().getEmail();
        String candidateName = assessment.getCandidate().getUser().getName();
        String invitationLink = frontendBaseUrl + "/assessment/access/" + assessment.getInvitationToken();
        try {
            emailService.sendReminder(candidateEmail, candidateName, invitationLink);
        } catch (Exception ex) {
            log.error("Failed to send reminder email for assessment {}: {}", assessmentId, ex.getMessage(), ex);
        }
    }

    @Transactional(readOnly = true)
    public List<UUID> getSeenQuestionIds(UUID candidateId) {
        return fetchSeenQuestionIds(candidateId);
    }

    @Transactional(readOnly = true)
    public List<AssessmentResponse> getCandidateAssessments(UUID candidateId) {
        return assessmentRepository.findByCandidateId(candidateId).stream()
                .map(a -> new AssessmentResponse(
                        a.getId(),
                        candidateId,
                        a.getStatus().name(),
                        a.getInvitationToken() != null ? frontendBaseUrl + "/assessment/access/" + a.getInvitationToken() : null,
                        a.getTimeLimitMinutes(),
                        a.getCreatedAt() != null ? a.getCreatedAt().toString() : null
                ))
                .toList();
    }

    @Transactional
    public AssessmentResponse submit(UUID assessmentId, UUID requestingUserId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new NoSuchElementException("Assessment not found: " + assessmentId));

        if (!assessment.getCandidate().getId().equals(requestingUserId)) {
            throw new AccessDeniedException("Forbidden: you are not the assigned candidate");
        }
        if (assessment.getStatus() == AssessmentStatus.SUBMITTED) {
            throw new ConflictException("Assessment has already been submitted");
        }
        if (assessment.getStatus() != AssessmentStatus.IN_PROGRESS) {
            throw new ConflictException("Assessment is not in progress");
        }

        boolean autoSubmit = false;
        if (assessment.getStartTime() != null) {
            Instant deadline = assessment.getStartTime()
                    .plusSeconds((long) assessment.getTimeLimitMinutes() * 60);
            autoSubmit = Instant.now().isAfter(deadline);
        }

        assessment.setAutoSubmitted(autoSubmit);
        assessment.setStatus(AssessmentStatus.SUBMITTED);
        Assessment saved = assessmentRepository.save(assessment);

        responseService.autoMarkMcqResponses(assessmentId);

        return toResponse(saved);
    }

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void autoSubmitExpiredAssessments() {
        List<Assessment> expired = assessmentRepository.findExpiredInProgress();
        for (Assessment assessment : expired) {
            assessment.setStatus(AssessmentStatus.SUBMITTED);
            assessment.setAutoSubmitted(true);
            assessmentRepository.save(assessment);
            responseService.autoMarkMcqResponses(assessment.getId());
            log.info("Auto-submitted expired assessment {}", assessment.getId());
        }
    }

    @Transactional
    public void close(UUID assessmentId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new NoSuchElementException("Assessment not found: " + assessmentId));
        if (assessment.getStatus() == AssessmentStatus.CLOSED) {
            throw new ConflictException("Assessment is already closed");
        }
        if (assessment.getStatus() != AssessmentStatus.SUBMITTED) {
            throw new ConflictException("Only submitted assessments can be closed");
        }
        assessment.setStatus(AssessmentStatus.CLOSED);
        assessmentRepository.save(assessment);
    }

    @Transactional
    public void finalise(UUID assessmentId, String overallFeedback) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new NoSuchElementException("Assessment not found: " + assessmentId));

        if (assessment.getStatus() == AssessmentStatus.MARKED) {
            throw new ConflictException("Assessment has already been marked");
        }
        if (assessment.getStatus() != AssessmentStatus.SUBMITTED) {
            throw new ConflictException("Assessment is not in SUBMITTED state");
        }

        List<UUID> emptyFeedbackQuestionIds = feedbackRepository.findQuestionsWithEmptyFeedback(assessmentId);
        if (!emptyFeedbackQuestionIds.isEmpty()) {
            throw new ValidationException("Feedback missing for questions: " + emptyFeedbackQuestionIds);
        }

        List<Feedback> allFeedback = feedbackRepository.findByAssessmentId(assessmentId);
        allFeedback.forEach(f -> f.setFinalised(true));
        feedbackRepository.saveAll(allFeedback);

        assessment.setStatus(AssessmentStatus.MARKED);
        assessmentRepository.save(assessment);

        try {
            emailService.sendFeedback(
                    assessment.getCandidate().getUser().getEmail(),
                    assessment.getCandidate().getUser().getName(),
                    overallFeedback != null ? overallFeedback : "");
        } catch (Exception ex) {
            log.error("Failed to send feedback email for assessment {}: {}", assessmentId, ex.getMessage(), ex);
        }
    }

    private AssessmentResponse toResponse(Assessment assessment) {
        String invitationLink = frontendBaseUrl + "/assessment/access/" + assessment.getInvitationToken();
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
