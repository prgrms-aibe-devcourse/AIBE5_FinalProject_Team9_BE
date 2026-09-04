package com.grimgate.grimgate_backend.domain.mypage.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyPageMainResponse {

    private MyPageProfileResponse profile;

    private MyPageStatsResponse stats;
}
