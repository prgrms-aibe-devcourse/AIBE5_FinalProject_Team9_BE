package com.grimgate.grimgate_backend.domain.theme.dto;

import com.grimgate.grimgate_backend.domain.theme.entity.Theme;
import lombok.AllArgsConstructor;
import lombok.Getter;

//서버 → 프론트로 보내주는 응답 DTO
@Getter
@AllArgsConstructor
public class ThemeResponse {
    private Long id;
    private Long branchId;
    private String thumbnailUrl;
    private String branchName;
    private String title;
    private Integer difficulty;
    private Integer horrorLevel;
    private Double rating;
    private Integer reviewCount;
    private Integer minPeople;
    private Integer maxPeople;
    private String tags;
    private Integer playTime;
    private String description;
    private Integer price;
    private String createdAt;

    public static ThemeResponse from(Theme theme) {
        return new ThemeResponse(
                theme.getId(),
                theme.getBranch().getId(),
                theme.getThumbnailUrl(),
                theme.getBranch().getBranchName(), // Branch에서 name 꺼내기
                theme.getTitle(),
                theme.getDifficulty(),
                theme.getHorrorLevel(),
                theme.getRating(),
                theme.getReviewCount(),
                theme.getMinPeople(),
                theme.getMaxPeople(),
                theme.getTags(),
                theme.getPlayTime(),
                theme.getDescription(),
                theme.getPrice(),
                theme.getCreatedAt() != null ? theme.getCreatedAt().toString() : null
        );
    }
}
