package com.cliniradar.dto;

public record ArticleResponseDto(
        String source,
        String sourceId,
        String title,
        String publishedAt,
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
