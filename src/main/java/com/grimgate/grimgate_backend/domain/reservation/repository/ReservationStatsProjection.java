package com.grimgate.grimgate_backend.domain.reservation.repository;

/**
4:  * 예약 통계 집계 결과를 매핑하기 위한 Spring Data JPA Projection 인터페이스입니다.
5:  */
public interface ReservationStatsProjection {
    Long getTotalCount();
    Long getTodayCount();
    Long getCompletedCount();
    Long getConfirmedCount();
    Long getCancelledCount();
}
