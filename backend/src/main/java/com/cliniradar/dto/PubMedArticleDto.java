package com.cliniradar.dto;

import java.time.LocalDate;

public record PubMedArticleDto(
        String pubmedId,
        String title,
        String abstractText,
        String journal,
        LocalDate publishedAt,
        String publicationType,
        String url
) {
}
