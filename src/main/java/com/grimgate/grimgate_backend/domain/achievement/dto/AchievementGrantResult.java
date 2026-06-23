package com.grimgate.grimgate_backend.domain.achievement.dto;

import com.grimgate.grimgate_backend.domain.achievement.entity.Achievement;

public record AchievementGrantResult(
    boolean newAcquired,
    Achievement achievement
) {}
