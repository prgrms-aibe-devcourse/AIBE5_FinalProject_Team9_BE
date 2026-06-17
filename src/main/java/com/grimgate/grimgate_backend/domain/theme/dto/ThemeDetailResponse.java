package com.grimgate.grimgate_backend.domain.theme.dto;

import com.grimgate.grimgate_backend.global.response.TabCommonResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

//테마 상세페이지 전용 응답 DTO
@Getter
public class ThemeDetailResponse extends TabCommonResponse {
    //상세정보 탭
    private Long branchId;
    private String branchCode;
    private String branchName;
    private String region;
    private Integer difficulty;
    private Integer horrorLevel;
    private Integer price;
    private String description;
    private Integer ageLimit;

    public ThemeDetailResponse(Double rating, Integer reviewCount, Integer minPeople,
                               Integer maxPeople, Integer playTime,String thumbnailUrl,
                               Long branchId, String branchCode, String branchName, String region,
                              Integer difficulty,Integer horrorLevel, Integer price, String description,
                              Integer ageLimit) {
        super(rating, reviewCount, minPeople, maxPeople, playTime, thumbnailUrl);
        this.branchId = branchId;
        this.branchCode = branchCode;
        this.branchName = branchName;
        this.region = region;
        this.difficulty = difficulty;
        this.horrorLevel = horrorLevel;
        this.price = price;
        this.description = description;
        this.ageLimit = ageLimit;
    }


}
