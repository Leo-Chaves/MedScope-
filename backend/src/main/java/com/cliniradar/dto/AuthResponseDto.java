package com.cliniradar.dto;

public record AuthResponseDto(
        String token,
        String name,
        String crm
) {
}
