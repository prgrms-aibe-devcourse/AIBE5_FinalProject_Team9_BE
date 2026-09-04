package com.grimgate.grimgate_backend.domain.theme.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ThemeCreateRequest {
    //테마명, 난이도,공포도,최대인원, 연령제한, 장르,테마 설명, 대표이미지

    @NotBlank
    private String title;

    @NotNull
    @Min(1) @Max(5)
    private Integer difficulty;

    @NotNull
    @Min(1) @Max(5)
    private Integer horrorLevel;

    @NotNull
    @Min(1) @Max(6)
    private Integer minPeople;

    @NotNull
    @Min(1) @Max(6)
    private Integer maxPeople;

    @NotNull
    private Integer ageLimit;

    @NotNull
    private Integer playTime;
    private String tags;
    private Integer price;

    @NotBlank
    private String description;



}
