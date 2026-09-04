package com.grimgate.grimgate_backend.domain.owner.dto;

import com.grimgate.grimgate_backend.domain.reservation.entity.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerReservationSearchRequest {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    private Long themeId;
    private String nickname;
    private ReservationStatus status;

    public String getNickname() {
        if (nickname != null && nickname.trim().isEmpty()) {
            return null;
        }
        return nickname;
    }
}
