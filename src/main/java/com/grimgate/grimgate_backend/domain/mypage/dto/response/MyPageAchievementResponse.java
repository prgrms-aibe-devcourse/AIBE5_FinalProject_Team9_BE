package com.grimgate.grimgate_backend.domain.mypage.dto.response;

import com.grimgate.grimgate_backend.domain.achievement.entity.AchievementConditionType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MyPageAchievementResponse {

    private Long id;

    private String name;

    private String description;

    // 업적 달성 조건 타입
    private AchievementConditionType conditionType;

    // 업적 달성 조건 값
    private Integer conditionValue;

    // 미획득이면 null
    private LocalDateTime acquiredAt;

    private boolean isAcquired;
}
