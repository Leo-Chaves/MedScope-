package com.cliniradar.dto;

import java.util.List;

public record SearchResponseDto(
        String cid,
        String condition,
        String queryUsed,
        String disclaimer,
        List<ArticleResponseDto> articles
) {
}
