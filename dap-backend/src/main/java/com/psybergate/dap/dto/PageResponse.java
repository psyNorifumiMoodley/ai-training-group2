package com.psybergate.dap.dto;

import java.util.List;

public record PageResponse<T>(List<T> content, long totalElements, int totalPages) {
}
