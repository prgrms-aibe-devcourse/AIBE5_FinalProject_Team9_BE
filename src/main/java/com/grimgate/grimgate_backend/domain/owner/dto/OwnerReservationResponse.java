package com.grimgate.grimgate_backend.domain.owner.dto;

import com.grimgate.grimgate_backend.domain.reservation.entity.Reservation;
import com.grimgate.grimgate_backend.domain.reservation.entity.ReservationStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class OwnerReservationResponse {

    private Long reservationId;
    private LocalDate reservationDate;
    private LocalTime reservationTime;
    private String themeTitle;
    private String nickname;
    private String phone;
    private Integer peopleCount;
    private ReservationStatus status;
    private String escapeResult;

    public static OwnerReservationResponse from(Reservation reservation) {
        String derivedResult = formatEscapeResult(reservation.getIsCleared(), reservation.getClearTime());

        return OwnerReservationResponse.builder()
                .reservationId(reservation.getId())
                .reservationDate(reservation.getTimeSlot().getSlotDate())
                .reservationTime(reservation.getTimeSlot().getStartTime())
                .themeTitle(reservation.getTimeSlot().getTheme().getTitle())
                .nickname(reservation.getMember().getAccount().getNickname())
                .phone(reservation.getMember().getAccount().getPhone())
                .peopleCount(reservation.getPeopleCount())
                .status(reservation.getStatus())
                .escapeResult(derivedResult)
                .build();
    }

    private static String formatEscapeResult(Boolean isCleared, LocalTime clearTime) {
        if (isCleared == null) {
            return "결과 미입력";
        }
        if (isCleared) {
            if (clearTime != null) {
                DateTimeFormatter formatter;
                if (clearTime.getHour() == 0) {
                    formatter = DateTimeFormatter.ofPattern("mm:ss");
                } else {
                    formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                }
                return "성공 (" + clearTime.format(formatter) + ")";
            }
            return "성공";
        }
        return "실패";
    }
}
