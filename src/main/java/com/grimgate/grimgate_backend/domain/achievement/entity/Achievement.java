package com.grimgate.grimgate_backend.domain.achievement.entity;

import com.grimgate.grimgate_backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 업적 엔티티
 */
@Entity
@Table(name = "achievement")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Achievement extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // 업적 달성 조건 타입
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AchievementConditionType conditionType;

    // 업적 달성 조건 값
    @Column(nullable = false)
    private Integer conditionValue;
}
