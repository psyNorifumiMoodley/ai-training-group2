package com.psybergate.dap.service;

import com.psybergate.dap.domain.Assessment;
import com.psybergate.dap.domain.AssessmentQuestion;
import com.psybergate.dap.domain.AssessmentStatus;
import com.psybergate.dap.domain.ConflictException;
import com.psybergate.dap.domain.DocResponse;
import com.psybergate.dap.domain.McqPlusResponse;
import com.psybergate.dap.domain.McqQuestion;
import com.psybergate.dap.domain.McqResponse;
import com.psybergate.dap.domain.QuestionGroupResponse;
import com.psybergate.dap.domain.Response;
import com.psybergate.dap.domain.TextResponse;
import com.psybergate.dap.dto.DocResponseRequest;
import com.psybergate.dap.dto.GroupResponseRequest;
import com.psybergate.dap.dto.McqPlusResponseRequest;
import com.psybergate.dap.dto.McqResponseRequest;
import com.psybergate.dap.dto.ResponseRequest;
import com.psybergate.dap.dto.TextResponseRequest;
import com.psybergate.dap.repository.AssessmentQuestionRepository;
import com.psybergate.dap.repository.AssessmentRepository;
import com.psybergate.dap.repository.McqQuestionRepository;
import com.psybergate.dap.repository.ResponseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ResponseService {

    private final ResponseRepository responseRepository;
    private final McqQuestionRepository mcqQuestionRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;

    public ResponseService(ResponseRepository responseRepository,
                           McqQuestionRepository mcqQuestionRepository,
                           AssessmentRepository assessmentRepository,
                           AssessmentQuestionRepository assessmentQuestionRepository) {
        this.responseRepository = responseRepository;
        this.mcqQuestionRepository = mcqQuestionRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentQuestionRepository = assessmentQuestionRepository;
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
            if (groupReq.childResponses() != null) {
                groupResponse.getChildResponses().clear();
                for (Map.Entry<UUID, ResponseRequest> entry : groupReq.childResponses().entrySet()) {
                    AssessmentQuestion childQuestion = assessmentQuestionRepository.findById(entry.getKey())
                            .orElseThrow(() -> new NoSuchElementException(
                                    "Child question not found: " + entry.getKey()));
                    Response child = upsertResponse(null, assessment, childQuestion, entry.getValue());
                    groupResponse.getChildResponses().add(child);
                }
            }
            return groupResponse;
        }
        throw new UnsupportedOperationException("Unsupported response type: " + request.getClass().getSimpleName());
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
