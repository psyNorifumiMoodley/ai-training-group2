package com.psybergate.dap.dto;

import java.util.UUID;

public record CandidateResponse(UUID id, String name, String email) {
}
