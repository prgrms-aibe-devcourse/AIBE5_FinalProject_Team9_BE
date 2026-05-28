package com.grimgate.grimgate_backend.domain.theme.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

//서버 → 프론트로 보내주는 응답 DTO
@Getter
@AllArgsConstructor
public class ThemeResponse {
    private Long id;
    private String thumbnailUrl;
    private String branchName;
    private String title;
    private Integer difficulty;
    private Integer horrorLevel;
    private Double rating;
    private Integer reviewCount;
    private Integer minPeople;
    private Integer maxPeople;
    private Integer playTime;
    private String description;
}
