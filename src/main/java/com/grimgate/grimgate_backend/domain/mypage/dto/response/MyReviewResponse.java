package com.grimgate.grimgate_backend.domain.mypage.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class MyReviewResponse {
    private Long reviewId;
    private String themeTitle;
    private Long themeId;
    private String nickname;
    private Integer rating;
    private Integer horrorRating;
    private Integer difficultyRating;
    private String tags;
    private String content;
    private Boolean spoiler;
    private LocalDateTime createdAt;
    private List<String> imageUrls;
    private LocalDateTime visitedAt;

    @Builder
    public MyReviewResponse(
            Long reviewId,
            String themeTitle,
            Long themeId,
            String nickname,
            Integer rating,
            Integer horrorRating,
            Integer difficultyRating,
            String tags,
            String content,
            Boolean spoiler,
            LocalDateTime createdAt,
            List<String> imageUrls,
            LocalDateTime visitedAt
    ) {
        this.reviewId = reviewId;
        this.themeTitle = themeTitle;
        this.themeId = themeId;
        this.nickname = nickname;
        this.rating = rating;
        this.horrorRating = horrorRating;
        this.difficultyRating= difficultyRating;
        this.tags = tags;
        this.content = content;
        this.spoiler = spoiler;
        this.createdAt = createdAt;
        this.imageUrls = imageUrls;
        this.visitedAt = visitedAt;
    }
}
