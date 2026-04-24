package com.cliniradar.dto;

import java.time.OffsetDateTime;

public record ErrorResponseDto(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message
) {
}
