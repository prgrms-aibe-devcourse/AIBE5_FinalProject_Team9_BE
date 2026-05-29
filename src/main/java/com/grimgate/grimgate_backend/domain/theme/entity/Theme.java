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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 방탈출 테마 정보를 관리하는 엔티티입니다.
 *
 * [빠른예약 조회에서 이 엔티티가 필요한 이유]
 * - 사용자는 빠른예약 화면에서 지역, 난이도, 공포도, 인원 수, 평점 등을 기준으로
 *   예약 가능한 테마를 조회합니다.
 * - 따라서 Theme 엔티티는 단순히 테마명만 가지는 것이 아니라,
 *   검색 조건과 응답 화면에 필요한 테마 상세 정보를 함께 관리해야 합니다.
 *
 * [Branch와의 관계]
 * - 하나의 지점(Branch)은 여러 개의 테마를 가질 수 있습니다.
 * - 하나의 테마는 반드시 하나의 지점에 소속됩니다.
 * - 빠른예약 조회 시 지역(region), 지점명(branchName)을 함께 응답해야 하므로
 *   Branch와 다대일 관계를 가집니다.
 */
@Entity
@Table(name = "theme")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Theme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 테마가 소속된 지점입니다.
     *
     * 빠른예약 조회에서 지역 필터와 지점명 응답에 사용됩니다.
     * 예: 서울 지역 테마만 조회, 강남점 테마 조회 등
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    /**
     * 테마명입니다.
     *
     * 빠른예약 목록, 테마 상세 페이지, 예약 화면에서 사용자에게 노출됩니다.
     */
    @Column(nullable = false)
    private String title;

    /**
     * 테마 설명입니다.
     *
     * 테마 상세 페이지와 목록 응답에서 테마의 분위기나 소개 문구로 사용됩니다.
     */
    @Column(nullable = false)
    private String description;

    /**
     * 테마 태그입니다.
     *
     * 예: 공포, 초보추천, 스릴러 등
     * 이후 검색 또는 화면 표시용으로 활용할 수 있습니다.
     */
    private String tags;

    /**
     * 공포도입니다.
     *
     * 빠른예약 필터 조건으로 사용됩니다.
     * 예: horrorLevel이 높은 테마만 조회
     */
    @Column(name = "horror_level", nullable = false)
    private Integer horrorLevel;

    /**
     * 난이도입니다.
     *
     * 빠른예약 필터 조건으로 사용됩니다.
     */
    @Column(nullable = false)
    private Integer difficulty;

    /**
     * 이용 가능 연령 제한입니다.
     *
     * 예약 생성 시 사용자 나이 검증에 활용될 수 있습니다.
     */
    @Column(name = "age_limit", nullable = false)
    private Integer ageLimit;

    /**
     * 플레이 시간입니다.
     *
     * 테마 상세 정보와 예약 가능 시간 안내에 사용됩니다.
     */
    @Column(name = "play_time", nullable = false)
    private Integer playTime;

    /**
     * 최소 예약 가능 인원입니다.
     *
     * 빠른예약에서 사용자가 선택한 인원 수가
     * minPeople 이상인지 검증할 때 사용됩니다.
     */
    @Column(name = "min_people", nullable = false)
    private Integer minPeople;

    /**
     * 최대 예약 가능 인원입니다.
     *
     * 빠른예약에서 사용자가 선택한 인원 수가
     * maxPeople 이하인지 검증할 때 사용됩니다.
     */
    @Column(name = "max_people", nullable = false)
    private Integer maxPeople;

    /**
     * 1인 기준 또는 테마 기준 가격입니다.
     *
     * 빠른예약 목록 및 예약 금액 계산에 사용됩니다.
     */
    @Column(nullable = false)
    private Integer price;

    /**
     * 테마 평점입니다.
     *
     * 빠른예약 기본 정렬인 rating_desc에서 사용됩니다.
     * 현재 develop 기준과 충돌을 줄이기 위해 Double 타입을 사용합니다.
     */
    private Double rating;

    /**
     * 리뷰 개수입니다.
     *
     * 빠른예약 목록에서 평점과 함께 사용자 신뢰도를 보여주는 정보로 사용됩니다.
     */
    @Column(name = "review_count")
    private Integer reviewCount;

    /**
     * 테마 썸네일 이미지 URL입니다.
     *
     * 빠른예약 목록 카드와 테마 상세 페이지에서 사용됩니다.
     */
    @Column(name = "thumbnail_url", nullable = false)
    private String thumbnailUrl;

    /**
     * 테마 데이터 생성 시각입니다.
     *
     * Hibernate가 엔티티 최초 저장 시 자동으로 값을 채웁니다.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 테마 데이터 수정 시각입니다.
     *
     * Hibernate가 엔티티 수정 시 자동으로 값을 갱신합니다.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 테스트 코드 및 초기 데이터 구성에서 Theme 객체를 생성하기 위한 Builder 생성자입니다.
     *
     * 빠른예약 단위 테스트에서 Branch, rating, 인원 범위, 난이도, 공포도 등을 가진
     * Theme 테스트 객체를 만들기 위해 사용됩니다.
     */
    @Builder
    public Theme(
            Long id,
            Branch branch,
            String title,
            String description,
            String tags,
            Integer horrorLevel,
            Integer difficulty,
            Integer ageLimit,
            Integer playTime,
            Integer minPeople,
            Integer maxPeople,
            Integer price,
            Double rating,
            Integer reviewCount,
            String thumbnailUrl
    ) {
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
    }
}