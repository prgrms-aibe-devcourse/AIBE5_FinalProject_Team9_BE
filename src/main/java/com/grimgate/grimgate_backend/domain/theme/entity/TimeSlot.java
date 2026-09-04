package com.grimgate.grimgate_backend.domain.theme.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 방탈출 테마의 특정 날짜와 시간대에 운영되는 예약 슬롯을 나타내는 엔티티입니다.
 * DB 테이블 'time_slot'과 매핑됩니다.
 * 
 * [예약 가능 시간 조회가 예약 생성 전에 필요한 이유]
 * - 방탈출과 같은 실시간 오프라인 서비스는 공간과 시간의 제약이 있습니다. 
 *   특정 테마에 대해 동일한 시간대에 한 팀만 이용이 가능하므로, 중복 예약을 사전에 원천 차단해야 합니다.
 * - 사용자가 예약을 완료(결제)하기 이전 단계에서 "현재 예약 가능한 비어있는 슬롯"을 실시간으로 확인하고 
 *   유효한 시간대를 선택할 수 있도록 유도하기 위해 조회가 필수적으로 선행되어야 합니다.
 * 
 * [상태값을 응답에 포함해야 하는 이유]
 * - 슬롯의 상태(AVAILABLE, HELD, FULL)를 사용자에게 실시간으로 제공하여 중복 클릭과 예약을 예방합니다.
 * - 사용자가 결제를 진행 중인 임시 선점 상태(HELD)와 이미 예약이 만료된 상태(FULL)를 명확히 구분함으로써, 
 *   고객이 예약을 진행하다가 마지막 결제 단계에서 실패하는 불쾌한 경험을 예방하여 UX를 개선할 수 있습니다.
 */
@Entity
@Table(name = "time_slot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id", nullable = false)
    private Theme theme;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TimeSlotStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public TimeSlot(Long id, Theme theme, LocalDate slotDate, LocalTime startTime, LocalTime endTime, TimeSlotStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.theme = theme;
        this.slotDate = slotDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        this.updatedAt = updatedAt == null ? LocalDateTime.now() : updatedAt;
    }
}
