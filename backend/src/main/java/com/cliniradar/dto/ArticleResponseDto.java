package com.cliniradar.dto;

import java.time.LocalDate;

public record ArticleResponseDto(
        String source,
        String sourceId,
        String title,
        LocalDate publishedAt,
        String publicationType,
        String journal,
        String url,
        String summaryPt,
        String relevanceLevel,
        String evidenceType,
        String practicalImpact,
        String warningNote
) {
}
