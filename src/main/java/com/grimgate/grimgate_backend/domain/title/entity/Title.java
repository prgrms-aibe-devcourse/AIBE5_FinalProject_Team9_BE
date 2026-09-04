package com.grimgate.grimgate_backend.domain.title.entity;

import com.grimgate.grimgate_backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 칭호 엔티티
 */
@Entity
@Table(name = "title")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Title extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // 칭호 획득 조건: 최소 성공률
    private Integer minSuccessRate;

    // 칭호 획득 조건: 최대 성공률
    private Integer maxSuccessRate;

    // 칭호 획득 조건: 필요 클리어 횟수 (nullable)
    private Integer requiredClearCount;
}
