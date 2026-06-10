package com.psybergate.dap.dto;

import java.util.UUID;

public record QuestionBankResponse(
        UUID id,
        String name
) {
}
