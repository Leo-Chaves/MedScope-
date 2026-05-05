package com.cliniradar.dto;

public record SearchStreamEventDto(
        String type,
        Object payload,
        String message
) {

    public static SearchStreamEventDto meta(SearchResponseDto payload) {
        return new SearchStreamEventDto("meta", payload, null);
    }

    public static SearchStreamEventDto article(ArticleResponseDto payload) {
        return new SearchStreamEventDto("article", payload, null);
    }

    public static SearchStreamEventDto complete() {
        return new SearchStreamEventDto("complete", null, null);
    }

    public static SearchStreamEventDto error(String message) {
        return new SearchStreamEventDto("error", null, message);
    }
}
