package com.psybergate.dap.dto;

import java.util.Map;
import java.util.UUID;

public record GroupResponseRequest(Map<UUID, ResponseRequest> childResponses) implements ResponseRequest {
}
