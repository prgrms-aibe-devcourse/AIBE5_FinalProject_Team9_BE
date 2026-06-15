package com.grimgate.grimgate_backend.domain.mypage.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class MyPageReservationResponse {

    private Long reservationId;

    private String themeName;

    private String branchName;

    private LocalDate reservationDate;

    private LocalTime reservationTime;

    private int peopleCount;

    private String status;

    // 예정 예약이면 null
    private Boolean isCleared;

    // 예정 예약이면 null
    private Integer clearTime;

    private String themeImageUrl;

    private Integer horrorLevel;

    private Integer difficulty;
}
