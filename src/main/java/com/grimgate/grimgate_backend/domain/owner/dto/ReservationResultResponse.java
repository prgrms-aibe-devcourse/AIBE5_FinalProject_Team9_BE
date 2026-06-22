package com.grimgate.grimgate_backend.domain.owner.dto;

import com.grimgate.grimgate_backend.domain.reservation.entity.Reservation;
import com.grimgate.grimgate_backend.domain.reservation.entity.ReservationStatus;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReservationResultResponse {

    private Long reservationId;
    private ReservationStatus status;
    private Boolean isCleared;
    private LocalTime clearTime;

    public static ReservationResultResponse from(Reservation reservation) {
        return ReservationResultResponse.builder()
                .reservationId(reservation.getId())
                .status(reservation.getStatus())
                .isCleared(reservation.getIsCleared())
                .clearTime(reservation.getClearTime())
                .build();
    }
}
