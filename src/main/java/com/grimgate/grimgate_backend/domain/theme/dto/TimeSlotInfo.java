package com.grimgate.grimgate_backend.domain.theme.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 특정 예약 가능 타임슬롯의 시간 상세 정보를 담는 DTO 클래스입니다.
 * 
 * [이 DTO가 필요한 이유]
 * - AvailableSlotsResponse 내부의 availableSlots 목록을 구성하여,
 *   사용자가 예약 가능한 일자와 정확한 시작/종료 시간을 UI에 제공할 수 있도록 데이터 구조를 단순화합니다.
 */
@Getter
public class TimeSlotInfo {

    private final Long timeSlotId;
    private final LocalDate slotDate;
    private final LocalTime startTime;
    private final LocalTime endTime;

    @Builder
    public TimeSlotInfo(Long timeSlotId, LocalDate slotDate, LocalTime startTime, LocalTime endTime) {
        this.timeSlotId = timeSlotId;
        this.slotDate = slotDate;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
