package com.grimgate.grimgate_backend.global.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TabCommonResponse {

    private Double rating;
    private Integer reviewCount;
    private Integer minPeople;
    private Integer maxPeople;
    private Integer playTime;
    private String thumbnailUrl;
}
