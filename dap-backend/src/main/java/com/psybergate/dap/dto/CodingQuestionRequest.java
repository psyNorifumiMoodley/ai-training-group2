package com.psybergate.dap.dto;

import com.psybergate.dap.domain.CodingQuestionLanguage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CodingQuestionRequest(
        @NotEmpty List<UUID> questionBankIds,
        @NotBlank String question,
        @NotNull CodingQuestionLanguage language,
        List<@Valid TestCaseRequest> testCases
) implements QuestionRequest {}
