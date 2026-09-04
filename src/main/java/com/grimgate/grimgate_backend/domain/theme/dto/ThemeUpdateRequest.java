package com.grimgate.grimgate_backend.domain.theme.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ThemeUpdateRequest {
    private String title;
    private String description;
    private String tags;

    @Min(1) @Max(5)
    private Integer horrorLevel;

    @Min(1) @Max(5)
    private Integer difficulty;
    private Integer ageLimit;

    @Min(1) @Max(300)
    private Integer playTime;

    @Min(1) @Max(6)
    private Integer minPeople;

    @Min(1) @Max(6)
    private Integer maxPeople;

    @Min(0)
    private Integer price;
    private String thumbnailUrl;
}