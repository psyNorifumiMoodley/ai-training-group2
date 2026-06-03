package com.psybergate.dap.service;

import com.psybergate.dap.domain.McqQuestion;
import com.psybergate.dap.domain.McqResponse;
import com.psybergate.dap.domain.Response;
import com.psybergate.dap.repository.McqQuestionRepository;
import com.psybergate.dap.repository.ResponseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ResponseService {

    private final ResponseRepository responseRepository;
    private final McqQuestionRepository mcqQuestionRepository;

    public ResponseService(ResponseRepository responseRepository,
                           McqQuestionRepository mcqQuestionRepository) {
        this.responseRepository = responseRepository;
        this.mcqQuestionRepository = mcqQuestionRepository;
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
