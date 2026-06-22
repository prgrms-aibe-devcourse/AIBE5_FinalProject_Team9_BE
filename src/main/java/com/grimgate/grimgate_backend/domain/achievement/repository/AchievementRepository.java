package com.grimgate.grimgate_backend.domain.achievement.repository;

import com.grimgate.grimgate_backend.domain.achievement.entity.Achievement;
import com.grimgate.grimgate_backend.domain.achievement.entity.AchievementConditionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {

    List<Achievement> findByConditionType(AchievementConditionType conditionType);
}
