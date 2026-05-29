package com.grimgate.grimgate_backend.domain.theme.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BranchDetailResponse {
    private String branchName;
    private String region;
    private String operatingHours;
    private String phone;
    private String address;

}
