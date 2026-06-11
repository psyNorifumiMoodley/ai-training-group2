package com.psybergate.dap.dto;

import java.util.List;

public record McqPlusResponseRequest(List<String> selectedAnswers, String followUpAnswer) implements ResponseRequest {
}
