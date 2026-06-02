package com.psybergate.dap.dto;

import java.util.List;

public record McqResponseRequest(List<String> selectedAnswers) implements ResponseRequest {
}
