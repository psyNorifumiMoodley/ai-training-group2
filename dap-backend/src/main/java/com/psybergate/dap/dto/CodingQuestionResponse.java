package com.psybergate.dap.dto;

import com.psybergate.dap.domain.CodingQuestionLanguage;

import java.util.List;
import java.util.UUID;

public record CodingQuestionResponse(
        UUID id,
        String category,
        String question,
        CodingQuestionLanguage language,
        List<TestCaseResponse> testCases
) implements QuestionResponse {}
