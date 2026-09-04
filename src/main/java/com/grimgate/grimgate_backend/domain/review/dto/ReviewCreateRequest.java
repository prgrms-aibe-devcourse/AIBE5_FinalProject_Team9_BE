package com.grimgate.grimgate_backend.domain.review.dto;

//리뷰 작성 요청 DTO,사용자 리뷰 작성할 때 보내는 값

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReviewCreateRequest {

    @NotNull
    private Long reservationId; //theme, member 다 연결됨

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
