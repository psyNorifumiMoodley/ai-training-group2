package com.psybergate.dap.service;

import com.psybergate.dap.domain.*;
import com.psybergate.dap.dto.AssessmentRequest;
import com.psybergate.dap.dto.AssessmentResponse;
import com.psybergate.dap.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AssessmentService {

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

    private final CandidateRepository candidateRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final McqQuestionRepository mcqQuestionRepository;
    private final TextQuestionRepository textQuestionRepository;
    private final DocQuestionRepository docQuestionRepository;
    private final GroupQuestionRepository groupQuestionRepository;

    public AssessmentService(CandidateRepository candidateRepository,
                             AssessmentRepository assessmentRepository,
                             AssessmentQuestionRepository assessmentQuestionRepository,
                             McqQuestionRepository mcqQuestionRepository,
                             TextQuestionRepository textQuestionRepository,
                             DocQuestionRepository docQuestionRepository,
                             GroupQuestionRepository groupQuestionRepository) {
        this.candidateRepository = candidateRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentQuestionRepository = assessmentQuestionRepository;
        this.mcqQuestionRepository = mcqQuestionRepository;
        this.textQuestionRepository = textQuestionRepository;
        this.docQuestionRepository = docQuestionRepository;
        this.groupQuestionRepository = groupQuestionRepository;
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
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Assessment getOrThrow(UUID id) {
        return assessmentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Assessment not found: " + id));
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

    private AssessmentResponse toResponse(Assessment assessment) {
        return new AssessmentResponse(
                assessment.getId(),
                assessment.getCandidate().getId(),
                assessment.getStatus().name(),
                assessment.getInvitationToken(),
                assessment.getTimeLimitMinutes(),
                assessment.getCreatedAt() != null ? assessment.getCreatedAt().toString() : null
        );
    }
}
