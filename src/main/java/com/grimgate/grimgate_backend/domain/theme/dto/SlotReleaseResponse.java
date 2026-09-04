package com.grimgate.grimgate_backend.domain.theme.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 슬롯 임시 선점(HOLD) 해제 성공 시 응답 결과를 담기 위한 DTO 클래스입니다.
 */
@Getter
@Builder
public class SlotReleaseResponse {

    private final Long timeSlotId;
    private final boolean released;

    public SlotReleaseResponse(Long timeSlotId, boolean released) {
        this.timeSlotId = timeSlotId;
        this.released = released;
    }
}
