package com.grimgate.grimgate_backend.domain.reservation.entity;

import com.grimgate.grimgate_backend.domain.member.entity.Member;
import com.grimgate.grimgate_backend.domain.theme.entity.TimeSlot;
import com.grimgate.grimgate_backend.global.entity.BaseTimeEntity;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "reservation")
public class Reservation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_slot_id", nullable = false)
    private TimeSlot timeSlot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "people_count", nullable = false)
    private Integer peopleCount;

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status;

    @Column(name = "terms_agreed_at", nullable = false)
    private LocalDateTime termsAgreedAt;

    @Column(name = "is_cleared")
    private Boolean isCleared;

    @Column(name = "clear_time")
    private LocalTime clearTime;

    /**
     * 예약을 확정(CONFIRMED) 상태로 변경합니다.
     */
    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
    }

    // 예약을 취소(CANCELLED) 상태로 변경합니다.
    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
    }

    // 방탈출 결과를 기록하고 상태를 COMPLETED로 변경합니다.
    public void recordResult(Boolean isCleared, LocalTime clearTime) {
        this.isCleared = isCleared;
        this.clearTime = clearTime;
        this.status = ReservationStatus.COMPLETED;
    }
}
