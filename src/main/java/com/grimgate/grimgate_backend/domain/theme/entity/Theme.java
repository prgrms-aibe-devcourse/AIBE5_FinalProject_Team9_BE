package com.grimgate.grimgate_backend.domain.theme.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 방탈출 테마 정보를 나타내는 엔티티입니다.
 * DB 테이블 'theme'와 매핑됩니다.
 * 
 * [빠른예약 조회에서 이 엔티티가 필요한 이유]
 * - 빠른예약 필터링 시 사용자가 요청하는 '난이도(difficulty)', '공포도(horrorLevel)',
 *   '최저 평점(minRating)', '추천 인원수(peopleCount)' 조건이 테마 엔티티 내에 존재하므로,
 *   데이터베이스 조회 조건 설정 및 응답 데이터(테마명, 평점, 썸네일 등) 구성을 위해 테마 엔티티 확장이 필수적입니다.
 */
@Entity
@Table(name = "theme")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Theme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String tags;

    @Column(name = "horror_level")
    private Integer horrorLevel;

    private Integer difficulty;

    @Column(name = "age_limit")
    private Integer ageLimit;

    @Column(name = "play_time")
    private Integer playTime;

    @Column(name = "min_people")
    private Integer minPeople;

    @Column(name = "max_people")
    private Integer maxPeople;

    private Integer price;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(name = "review_count")
    private Integer reviewCount;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Theme(Long id, Branch branch, String title, String description, String tags,
                 Integer horrorLevel, Integer difficulty, Integer ageLimit, Integer playTime,
                 Integer minPeople, Integer maxPeople, Integer price, BigDecimal rating,
                 Integer reviewCount, String thumbnailUrl, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.branch = branch;
        this.title = title;
        this.description = description;
        this.tags = tags;
        this.horrorLevel = horrorLevel;
        this.difficulty = difficulty;
        this.ageLimit = ageLimit;
        this.playTime = playTime;
        this.minPeople = minPeople;
        this.maxPeople = maxPeople;
        this.price = price;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.thumbnailUrl = thumbnailUrl;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        this.updatedAt = updatedAt == null ? LocalDateTime.now() : updatedAt;
    }
}
