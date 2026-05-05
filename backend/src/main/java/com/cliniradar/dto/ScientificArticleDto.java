package com.cliniradar.dto;

import java.time.LocalDate;

public record ScientificArticleDto(
        String source,
        String sourceId,
        String title,
        String abstractText,
        String journal,
        LocalDate publishedAt,
        String publishedAtDisplay,
        String publicationType,
        String url
) {
}
