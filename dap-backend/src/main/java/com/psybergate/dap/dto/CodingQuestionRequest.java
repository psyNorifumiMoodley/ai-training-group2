package com.psybergate.dap.dto;

import com.psybergate.dap.domain.Language;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CodingQuestionRequest(
        @NotBlank String category,
        @NotBlank String question,
        @NotNull Language language,
        List<@Valid TestCaseRequest> testCases
) implements QuestionRequest {}
