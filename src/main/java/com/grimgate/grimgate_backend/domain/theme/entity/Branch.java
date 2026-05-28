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

/**
 * 방탈출 카페 지점 정보를 관리하는 엔티티입니다.
 * DB 테이블 'branch'와 매핑됩니다.
 * 
 * [빠른예약 조회에서 이 엔티티가 필요한 이유]
 * - 사용자는 특정 '지역(region)'에 있는 방탈출 테마들을 조회하고자 하므로,
 *   테마가 속한 지점의 지역 정보 필터링 및 응답에 지점명(branchName)을 포함하기 위해 지점 엔티티가 필수적입니다.
 */
@Entity
@Table(name = "branch")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "manager_id")
    private Long managerId;

    @Column(name = "branch_code", nullable = false)
    private String branchCode;

    @Column(name = "branch_name", nullable = false)
    private String branchName;

    @Column(name = "region", nullable = false)
    private String region;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "phone")
    private String phone;

    @Column(name = "operating_hours")
    private String operatingHours;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Branch(Long id, Long managerId, String branchCode, String branchName, String region,
                  String address, String phone, String operatingHours,
                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.managerId = managerId;
        this.branchCode = branchCode;
        this.branchName = branchName;
        this.region = region;
        this.address = address;
        this.phone = phone;
        this.operatingHours = operatingHours;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        this.updatedAt = updatedAt == null ? LocalDateTime.now() : updatedAt;
    }
}
