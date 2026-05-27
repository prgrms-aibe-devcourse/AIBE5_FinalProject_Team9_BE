package com.grimgate.grimgate_backend.domain.theme.dto;

import com.grimgate.grimgate_backend.domain.theme.entity.TimeSlot;
import com.grimgate.grimgate_backend.domain.theme.entity.TimeSlotStatus;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;

@Getter
public class TimeSlotResponse {

    private final Long timeSlotId;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final TimeSlotStatus status;

    @Builder
    public TimeSlotResponse(Long timeSlotId, LocalTime startTime, LocalTime endTime, TimeSlotStatus status) {
        this.timeSlotId = timeSlotId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    /**
     * TimeSlot 엔티티를 TimeSlotResponse DTO로 변환하는 정적 팩토리 메서드입니다.
     * 엔티티의 프레젠테이션 계층 노출을 방지하기 위해 사용됩니다.
     *
     * @param timeSlot 타임슬롯 엔티티
     * @return 변환된 DTO 객체
     */
    public static TimeSlotResponse from(TimeSlot timeSlot) {
        return TimeSlotResponse.builder()
                .timeSlotId(timeSlot.getId())
                .startTime(timeSlot.getStartTime())
                .endTime(timeSlot.getEndTime())
                .status(timeSlot.getStatus())
                .build();
    }
}
