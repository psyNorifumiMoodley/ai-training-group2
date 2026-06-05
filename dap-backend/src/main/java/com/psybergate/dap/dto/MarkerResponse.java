package com.psybergate.dap.dto;

import java.util.UUID;

public record MarkerResponse(UUID id, String name, String email, String createdAt) {
}
