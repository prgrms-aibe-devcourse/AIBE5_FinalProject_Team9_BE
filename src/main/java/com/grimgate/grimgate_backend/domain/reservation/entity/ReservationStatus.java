package com.grimgate.grimgate_backend.domain.reservation.entity;

/**
 * 예약의 상태를 나타내는 Enum 클래스입니다.
 */
public enum ReservationStatus {
    PENDING_PAYMENT, // 결제 대기
    CONFIRMED,       // 예약 확정
    CANCELLED,       // 예약 취소
    COMPLETED        // 이용 완료
}

