package com.grimgate.grimgate_backend.domain.theme.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 슬롯 임시 선점(HOLD) 요청을 받기 위한 DTO 클래스입니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SlotHoldRequest {

    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long memberId;
}
