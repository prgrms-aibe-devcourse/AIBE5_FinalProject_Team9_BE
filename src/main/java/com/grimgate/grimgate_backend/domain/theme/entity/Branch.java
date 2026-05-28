package com.grimgate.grimgate_backend.domain.theme.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 방탈출 카페 지점 정보를 관리하는 엔티티입니다.
 * DB 테이블 'branch'와 매핑됩니다.
 *
 * [빠른예약 조회에서 이 엔티티가 필요한 이유]
 * - 사용자는 특정 지역(region)에 있는 방탈출 테마를 조회합니다.
 * - 테마는 지점에 소속되어 있으므로, 빠른예약 조회 시 테마가 속한 지점의
 *   지역 정보와 지점명을 함께 응답하기 위해 Branch 엔티티가 필요합니다.
 *
 * [Theme와의 관계]
 * - 하나의 지점은 여러 개의 테마를 가질 수 있습니다.
 * - 하나의 테마는 반드시 하나의 지점에 소속됩니다.
 * - 실제 연관관계의 주인은 Theme 엔티티의 branch 필드입니다.
 */
@Entity
@Table(name = "branch")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 지점을 관리하는 매니저 ID입니다.
     *
     * 현재 단계에서는 Manager 엔티티와 직접 연관관계를 맺지 않고,
     * ERD 기준의 manager_id 값을 저장하는 방식으로 관리합니다.
     */
    @Column(name = "manager_id", nullable = false)
    private Long managerId;

    /**
     * 지점 고유 코드입니다.
     *
     * 지점을 식별하기 위한 비즈니스 코드입니다.
     * 같은 지점 코드가 중복되면 안 되므로 unique 조건을 둡니다.
     */
    @Column(name = "branch_code", nullable = false, unique = true)
    private String branchCode;

    /**
     * 지점명입니다.
     *
     * 예: 강남점, 홍대점
     * 빠른예약 목록과 테마 상세 응답에서 사용자에게 노출됩니다.
     */
    @Column(name = "branch_name", nullable = false)
    private String branchName;

    /**
     * 지점이 위치한 지역입니다.
     *
     * 빠른예약의 지역 필터 조건으로 사용됩니다.
     * 예: 서울, 경기, 부산
     */
    @Column(name = "region", nullable = false)
    private String region;

    /**
     * 지점 주소입니다.
     *
     * 테마 상세 페이지나 위치 안내 화면에서 사용됩니다.
     */
    @Column(name = "address", nullable = false)
    private String address;

    /**
     * 지점 연락처입니다.
     *
     * 예약 안내 또는 지점 상세 정보에서 사용됩니다.
     */
    @Column(name = "phone", nullable = false)
    private String phone;

    /**
     * 지점 운영 시간입니다.
     *
     * 위치 안내 또는 지점 상세 응답에서 사용됩니다.
     */
    @Column(name = "operating_hours", nullable = false)
    private String operatingHours;

    /**
     * 지점 데이터 생성 시각입니다.
     *
     * Hibernate가 엔티티 최초 저장 시 자동으로 값을 채웁니다.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 지점 데이터 수정 시각입니다.
     *
     * Hibernate가 엔티티 수정 시 자동으로 값을 갱신합니다.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 테스트 코드 및 초기 데이터 구성에서 Branch 객체를 생성하기 위한 Builder 생성자입니다.
     *
     * 빠른예약 단위 테스트에서 Theme가 소속될 지점 정보를 만들 때 사용됩니다.
     * createdAt, updatedAt은 Hibernate가 관리하므로 Builder에서 직접 받지 않습니다.
     */
    @Builder
    public Branch(
            Long id,
            Long managerId,
            String branchCode,
            String branchName,
            String region,
            String address,
            String phone,
            String operatingHours
    ) {
        this.id = id;
        this.managerId = managerId;
        this.branchCode = branchCode;
        this.branchName = branchName;
        this.region = region;
        this.address = address;
        this.phone = phone;
        this.operatingHours = operatingHours;
    }
}