package com.psybergate.dap.service;

import com.psybergate.dap.domain.Assessment;
import com.psybergate.dap.domain.AssessmentQuestion;
import com.psybergate.dap.domain.AssessmentStatus;
import com.psybergate.dap.domain.CodingQuestion;
import com.psybergate.dap.domain.CodingResponse;
import com.psybergate.dap.domain.ConflictException;
import com.psybergate.dap.domain.DocResponse;
import com.psybergate.dap.domain.McqPlusResponse;
import com.psybergate.dap.domain.McqQuestion;
import com.psybergate.dap.domain.McqResponse;
import com.psybergate.dap.domain.QuestionGroupResponse;
import com.psybergate.dap.domain.Response;
import com.psybergate.dap.domain.TestCase;
import com.psybergate.dap.domain.TestCaseResult;
import com.psybergate.dap.domain.TextResponse;
import com.psybergate.dap.dto.CodeExecuteResponse;
import com.psybergate.dap.dto.CodingResponseRequest;
import com.psybergate.dap.dto.DocResponseRequest;
import com.psybergate.dap.dto.GroupResponseRequest;
import com.psybergate.dap.dto.McqPlusResponseRequest;
import com.psybergate.dap.dto.McqResponseRequest;
import com.psybergate.dap.dto.ResponseRequest;
import com.psybergate.dap.dto.TestCaseResultResponse;
import com.psybergate.dap.dto.TextResponseRequest;
import com.psybergate.dap.repository.AssessmentQuestionRepository;
import com.psybergate.dap.repository.AssessmentRepository;
import com.psybergate.dap.repository.CodingQuestionRepository;
import com.psybergate.dap.repository.CodingResponseRepository;
import com.psybergate.dap.repository.McqQuestionRepository;
import com.psybergate.dap.repository.ResponseRepository;
import com.psybergate.dap.repository.TestCaseResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ResponseService {

    private static final Logger log = LoggerFactory.getLogger(ResponseService.class);

    private final ResponseRepository responseRepository;
    private final McqQuestionRepository mcqQuestionRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final CodingResponseRepository codingResponseRepository;
    private final CodeExecutionService codeExecutionService;
    private final TestCaseResultRepository testCaseResultRepository;
    private final CodingQuestionRepository codingQuestionRepository;

    public ResponseService(ResponseRepository responseRepository,
                           McqQuestionRepository mcqQuestionRepository,
                           AssessmentRepository assessmentRepository,
                           AssessmentQuestionRepository assessmentQuestionRepository,
                           CodingResponseRepository codingResponseRepository,
                           CodeExecutionService codeExecutionService,
                           TestCaseResultRepository testCaseResultRepository,
                           CodingQuestionRepository codingQuestionRepository) {
        this.responseRepository = responseRepository;
        this.mcqQuestionRepository = mcqQuestionRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentQuestionRepository = assessmentQuestionRepository;
        this.codingResponseRepository = codingResponseRepository;
        this.codeExecutionService = codeExecutionService;
        this.testCaseResultRepository = testCaseResultRepository;
        this.codingQuestionRepository = codingQuestionRepository;
    }

    @Transactional
    public void saveResponse(UUID assessmentId, UUID questionId, ResponseRequest request) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new NoSuchElementException("Assessment not found: " + assessmentId));

        AssessmentStatus status = assessment.getStatus();
        if (status == AssessmentStatus.SUBMITTED || status == AssessmentStatus.MARKED) {
            throw new ConflictException("Assessment has already been submitted and cannot be modified");
        }
        if (status != AssessmentStatus.IN_PROGRESS) {
            throw new ConflictException("Assessment is not in progress");
        }

        AssessmentQuestion question = assessmentQuestionRepository.findById(questionId)
                .orElseThrow(() -> new NoSuchElementException("Question not found: " + questionId));

        Optional<Response> existing = responseRepository.findByAssessmentIdAndQuestionId(assessmentId, questionId);

        Response response = upsertResponse(existing.orElse(null), assessment, question, request);
        responseRepository.save(response);
    }

    private Response upsertResponse(Response existing, Assessment assessment,
                                    AssessmentQuestion question, ResponseRequest request) {
        if (request instanceof McqPlusResponseRequest mcqPlusReq) {
            McqPlusResponse mcqPlusResponse = existing instanceof McqPlusResponse m ? m : new McqPlusResponse();
            mcqPlusResponse.setAssessment(assessment);
            mcqPlusResponse.setQuestion(question);
            mcqPlusResponse.setSelectedAnswers(mcqPlusReq.selectedAnswers());
            mcqPlusResponse.setCorrect(null);
            mcqPlusResponse.setFollowUpAnswer(mcqPlusReq.followUpAnswer());
            return mcqPlusResponse;
        }
        if (request instanceof McqResponseRequest mcqReq) {
            McqResponse mcqResponse = existing instanceof McqResponse m ? m : new McqResponse();
            mcqResponse.setAssessment(assessment);
            mcqResponse.setQuestion(question);
            mcqResponse.setSelectedAnswers(mcqReq.selectedAnswers());
            mcqResponse.setCorrect(null);
            return mcqResponse;
        }
        if (request instanceof TextResponseRequest textReq) {
            TextResponse textResponse = existing instanceof TextResponse t ? t : new TextResponse();
            textResponse.setAssessment(assessment);
            textResponse.setQuestion(question);
            textResponse.setAnswer(textReq.answer());
            return textResponse;
        }
        if (request instanceof DocResponseRequest docReq) {
            DocResponse docResponse = existing instanceof DocResponse d ? d : new DocResponse();
            docResponse.setAssessment(assessment);
            docResponse.setQuestion(question);
            docResponse.setFilePath(docReq.filePath());
            return docResponse;
        }
        if (request instanceof GroupResponseRequest groupReq) {
            QuestionGroupResponse groupResponse = existing instanceof QuestionGroupResponse g
                    ? g : new QuestionGroupResponse();
            groupResponse.setAssessment(assessment);
            groupResponse.setQuestion(question);
            if (groupReq.childAnswers() != null) {
                groupResponse.getChildResponses().clear();
                for (String answer : groupReq.childAnswers()) {
                    TextResponse child = new TextResponse();
                    child.setAssessment(assessment);
                    child.setQuestion(question);
                    child.setAnswer(answer);
                    groupResponse.getChildResponses().add(child);
                }
            }
            return groupResponse;
        }
        if (request instanceof CodingResponseRequest codingReq) {
            CodingResponse codingResponse = existing instanceof CodingResponse c ? c : new CodingResponse();
            codingResponse.setAssessment(assessment);
            codingResponse.setQuestion(question);
            codingResponse.setCode(codingReq.code());
            return codingResponse;
        }
        throw new UnsupportedOperationException("Unsupported response type: " + request.getClass().getSimpleName());
    }

    @Transactional
    public CodeExecuteResponse executeCode(UUID assessmentId, UUID questionId, UUID requestingUserId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new NoSuchElementException("Assessment not found: " + assessmentId));

        if (!assessment.getCandidate().getId().equals(requestingUserId)) {
            throw new AccessDeniedException("Forbidden: you are not the assigned candidate");
        }

        AssessmentStatus status = assessment.getStatus();
        if (status == AssessmentStatus.SUBMITTED || status == AssessmentStatus.MARKED) {
            throw new ConflictException("Assessment has already been submitted");
        }

        CodingResponse codingResponse = codingResponseRepository
                .findByAssessmentIdAndQuestionId(assessmentId, questionId)
                .orElseThrow(() -> new NoSuchElementException("No code saved for question: " + questionId));

        CodingQuestion codingQuestion = codingQuestionRepository.findById(questionId)
                .orElseThrow(() -> new NoSuchElementException("Coding question not found: " + questionId));

        List<TestCaseResultResponse> results = codeExecutionService.execute(codingQuestion, codingResponse.getCode());

        testCaseResultRepository.deleteAllByCodingResponseId(codingResponse.getId());
        persistTestCaseResults(codingResponse, codingQuestion, results);

        codingResponse.setExecutedAt(Instant.now());
        codingResponseRepository.save(codingResponse);

        return new CodeExecuteResponse(results, codingResponse.getExecutedAt());
    }

    @Transactional
    public void autoExecuteCodingResponses(UUID assessmentId) {
        List<Response> responses = responseRepository.findByAssessmentId(assessmentId);
        for (Response response : responses) {
            if (!(response instanceof CodingResponse codingResponse)) continue;
            try {
                CodingQuestion codingQuestion = codingQuestionRepository
                        .findById(codingResponse.getQuestion().getId())
                        .orElseThrow(() -> new NoSuchElementException(
                                "Coding question not found: " + codingResponse.getQuestion().getId()));

                List<TestCaseResultResponse> results = codeExecutionService.execute(
                        codingQuestion, codingResponse.getCode());

                testCaseResultRepository.deleteAllByCodingResponseId(codingResponse.getId());
                persistTestCaseResults(codingResponse, codingQuestion, results);

                codingResponse.setExecutedAt(Instant.now());
                codingResponseRepository.save(codingResponse);
            } catch (Exception ex) {
                log.error("Failed to auto-execute coding response {} for assessment {}: {}",
                        codingResponse.getId(), assessmentId, ex.getMessage(), ex);
            }
        }
    }

    private void persistTestCaseResults(CodingResponse codingResponse, CodingQuestion codingQuestion,
                                        List<TestCaseResultResponse> results) {
        Map<UUID, TestCase> testCaseById = codingQuestion.getTestCases().stream()
                .collect(Collectors.toMap(TestCase::getId, Function.identity()));

        List<TestCaseResult> entities = new ArrayList<>();
        for (TestCaseResultResponse r : results) {
            TestCase testCase = testCaseById.get(r.testCaseId());
            if (testCase == null) continue;
            entities.add(TestCaseResult.builder()
                    .codingResponse(codingResponse)
                    .testCase(testCase)
                    .passed(r.passed())
                    .actualOutput(r.actualOutput())
                    .executionTimeMs(r.executionTimeMs())
                    .memoryUsedMb(r.memoryUsedMb())
                    .errorMessage(r.errorMessage())
                    .ordinal(r.ordinal())
                    .build());
        }
        testCaseResultRepository.saveAll(entities);
    }

    @Transactional(readOnly = true)
    public List<Response> getResponsesForAssessment(UUID assessmentId) {
        return responseRepository.findByAssessmentId(assessmentId);
    }

    @Transactional
    public void autoMarkMcqResponses(UUID assessmentId) {
        List<Response> responses = responseRepository.findByAssessmentId(assessmentId);
        for (Response response : responses) {
            if (response instanceof McqResponse mcqResponse) {
                McqQuestion question = mcqQuestionRepository
                        .findById(mcqResponse.getQuestion().getId())
                        .orElseThrow(() -> new NoSuchElementException(
                                "McqQuestion not found: " + mcqResponse.getQuestion().getId()));
                Set<String> correct = new HashSet<>(question.getCorrectAnswers());
                Set<String> selected = new HashSet<>(
                        mcqResponse.getSelectedAnswers() != null ? mcqResponse.getSelectedAnswers() : List.of());
                mcqResponse.setCorrect(correct.equals(selected));
                responseRepository.save(mcqResponse);
            }
        }
    }
}
