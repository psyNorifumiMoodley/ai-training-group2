package com.psybergate.dap.dto;

import java.util.List;
import java.util.UUID;

public record GroupChildResponse(
        UUID id,
        String questionText,
        List<String> keywords,
        int marks
) {
}
