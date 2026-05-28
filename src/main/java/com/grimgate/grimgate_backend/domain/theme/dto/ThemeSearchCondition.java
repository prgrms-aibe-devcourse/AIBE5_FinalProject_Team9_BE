package com.grimgate.grimgate_backend.domain.theme.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ThemeSearchCondition {
    private String region;
    private Integer difficulty;
    private Integer min_people;
    private Integer maxPeople;
    private Integer horror_level;
    private Double min_rating; //평점
    private String sort;
    private Integer page;
    private Integer limit;
    private String keyword;
}
