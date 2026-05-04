package com.cliniradar.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SearchResponseDto(
        String cid,
        String condition,
        String queryUsed,
        LocalDateTime refreshedAt,
        String disclaimer,
        List<ArticleResponseDto> articles
) {
}
