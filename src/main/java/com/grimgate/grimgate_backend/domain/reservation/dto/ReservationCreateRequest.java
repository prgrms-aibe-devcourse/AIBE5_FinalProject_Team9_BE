package com.grimgate.grimgate_backend.domain.reservation.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 예약 생성 요청 데이터를 전달받는 DTO 클래스입니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReservationCreateRequest {

    @NotNull(message = "타임슬롯 ID는 필수입니다.")
    private Long timeSlotId;

    @NotBlank(message = "선점 토큰은 필수입니다.")
    private String holdToken;

    @NotNull(message = "예약 인원은 필수입니다.")
    @Min(value = 1, message = "예약 인원은 최소 1명 이상이어야 합니다.")
    private Integer peopleCount;

    @NotNull(message = "서비스 이용약관에 동의해야 합니다.")
    @AssertTrue(message = "서비스 이용약관에 동의해야 합니다.")
    private Boolean termsAgreed;
}
