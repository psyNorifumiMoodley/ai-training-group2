package com.psybergate.dap.dto;

import java.util.List;

public record McqAnswerPayload(List<String> selectedAnswers, List<String> allOptions, List<String> correctAnswers) {}
