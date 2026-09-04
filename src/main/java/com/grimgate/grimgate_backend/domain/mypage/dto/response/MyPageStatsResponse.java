package com.grimgate.grimgate_backend.domain.mypage.dto.response;

import lombok.Builder;
import lombok.Getter;
@Getter
@Builder
public class MyPageStatsResponse {

    // 완료된 예약 수
    private int totalPlayCount;

    // 성공률 (0~100)
    private int successRate;

    // 최단 클리어 타임(초), 없으면 null
    private Integer bestClearTime;

    private long acquiredAchievementCount;

    private long totalAchievementCount;
}
