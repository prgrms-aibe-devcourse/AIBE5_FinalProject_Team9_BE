package com.grimgate.grimgate_backend.domain.reservation.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 예약 생성 완료 결과를 담아 응답하기 위한 DTO 클래스입니다.
 */
@Getter
@Builder
public class ReservationCreateResponse {

    private final Long reservationId;
    private final Long timeSlotId;
    private final Long memberId;
    private final String status;
    private final Integer peopleCount;
    private final Integer totalPrice;

    public ReservationCreateResponse(Long reservationId, Long timeSlotId, Long memberId, String status, Integer peopleCount, Integer totalPrice) {
        this.reservationId = reservationId;
        this.timeSlotId = timeSlotId;
        this.memberId = memberId;
        this.status = status;
        this.peopleCount = peopleCount;
        this.totalPrice = totalPrice;
    }
}
