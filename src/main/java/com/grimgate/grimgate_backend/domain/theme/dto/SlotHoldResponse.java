package com.grimgate.grimgate_backend.domain.theme.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 슬롯 임시 선점(HOLD) 성공 시 응답 결과를 담기 위한 DTO 클래스입니다.
 */
@Getter
public class SlotHoldResponse {

    private final Long timeSlotId;
    private final String holdToken;
    private final long expiresInSeconds;

    @Builder
    public SlotHoldResponse(Long timeSlotId, String holdToken, long expiresInSeconds) {
        this.timeSlotId = timeSlotId;
        this.holdToken = holdToken;
        this.expiresInSeconds = expiresInSeconds;
    }
}
