package com.grimgate.grimgate_backend.domain.theme.dto;

import com.grimgate.grimgate_backend.global.response.TabCommonResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class BranchDetailResponse extends TabCommonResponse {
    private String branchName;
    private String storeName;
    private String region;
    private String operatingHours;
    private String phone;
    private String address;

    public BranchDetailResponse(Double rating, Integer reviewCount, Integer minPeople,
                                Integer maxPeople, Integer playTime, String thumbnailUrl,
                                String branchName, String storeName,String region, String operatingHours,
                                String phone, String address) {
        super(rating, reviewCount, minPeople, maxPeople, playTime, thumbnailUrl);
        this.branchName = branchName;
        this.storeName = storeName;
        this.region = region;
        this.operatingHours = operatingHours;
        this.phone = phone;
        this.address = address;
    }

}
