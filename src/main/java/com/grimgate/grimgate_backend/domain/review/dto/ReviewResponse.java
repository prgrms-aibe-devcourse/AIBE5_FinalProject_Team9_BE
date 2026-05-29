package com.grimgate.grimgate_backend.domain.review.dto;

//리뷰 조회 응답 DTO,프론트에 리뷰 보여줄 때 사용

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class ReviewResponse {
    private String nickname;
    private Integer rating;
    private Integer horrorRating;
    private Integer difficultyRating;
    private String tags;
    private String content;
    private Boolean spoiler;
    private LocalDateTime createdAt;

    @Builder
    public ReviewResponse(
            String nickname,
            Integer rating,
            Integer horrorRating,
            Integer difficultyRating,
            String tags,
            String content,
            Boolean spoiler,
            LocalDateTime createdAt
    ) {
        this.nickname = nickname;
        this.rating = rating;
        this.horrorRating = horrorRating;
        this.difficultyRating= difficultyRating;
        this.tags = tags;
        this.content = content;
        this.spoiler = spoiler;
        this.createdAt = createdAt;
    }
}
