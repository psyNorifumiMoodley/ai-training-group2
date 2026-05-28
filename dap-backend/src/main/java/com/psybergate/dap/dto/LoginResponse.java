package com.psybergate.dap.dto;

import com.psybergate.dap.domain.Role;

public record LoginResponse(String token, Role role) {
}
