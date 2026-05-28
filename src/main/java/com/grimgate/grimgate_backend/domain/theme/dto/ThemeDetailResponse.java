package com.grimgate.grimgate_backend.domain.theme.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

//테마 상세페이지 전용 응답 DTO
@Getter
@AllArgsConstructor
public class ThemeDetailResponse {
    //상세정보 탭
    private String branchCode;
    private String branchName;
    private String region;
    private String address;
    private String phone;
    private String operatingHours;
    private Integer minPeople;
    private Integer maxPeople;
    private String description;
    private Integer playTime;


}
