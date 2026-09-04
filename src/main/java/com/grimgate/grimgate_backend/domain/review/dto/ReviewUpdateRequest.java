package com.grimgate.grimgate_backend.domain.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

//리뷰 수정
@Getter
@NoArgsConstructor
public class ReviewUpdateRequest {
    @NotNull
    @Min(1) @Max(5)
    private Integer rating;

    @NotNull @Min(1) @Max(5)
    private Integer horrorRating;

    @NotNull @Min(1) @Max(5)
    private Integer difficultyRating;

    private String tags;

    @NotBlank
    private String content;

    @NotNull
    private Boolean spoiler;

}
