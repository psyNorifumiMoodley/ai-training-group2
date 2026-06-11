package com.psybergate.dap.dto;

import java.util.List;

public record GroupResponseRequest(List<String> childAnswers) implements ResponseRequest {
}
