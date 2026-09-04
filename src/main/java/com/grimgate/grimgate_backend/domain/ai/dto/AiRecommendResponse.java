package com.grimgate.grimgate_backend.domain.ai.dto;

import com.grimgate.grimgate_backend.domain.theme.entity.Theme;

import java.util.List;

public record AiRecommendResponse(
        String type,        // "recommendation" or "message"
        String message,
        List<ThemeCard> themes
) {
    public record ThemeCard(
            Long id,
            String title,
            String thumbnailUrl,
            Integer horrorLevel,
            Integer difficulty,
            Double rating,
            String description
    ) {
        public static ThemeCard from(Theme theme) {
            return new ThemeCard(
                    theme.getId(),
                    theme.getTitle(),
                    theme.getThumbnailUrl(),
                    theme.getHorrorLevel(),
                    theme.getDifficulty(),
                    theme.getRating(),
                    theme.getDescription()

            );
        }
    }

    // 정상 추천 응답
    public static AiRecommendResponse recommend(String message, List<Theme> themes) {
        return new AiRecommendResponse(
                "recommendation",
                message,
                themes.stream().map(ThemeCard::from).toList()
        );
    }

    // 일반 대화 응답 (추천 아닌 경우)
    public static AiRecommendResponse message(String message) {
        return new AiRecommendResponse("message", message, List.of());
    }

    // Gemini 실패 시 fallback 응답
    public static AiRecommendResponse fallback(List<Theme> themes) {
        return new AiRecommendResponse(
                "recommendation",
                "AI 추천이 일시적으로 불가능해 추천 테마 목록을 제공해드려요.",
                themes.stream().map(ThemeCard::from).toList()
        );
    }
}