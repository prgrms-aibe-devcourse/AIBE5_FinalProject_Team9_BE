package com.grimgate.grimgate_backend.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PaymentRefundRequest {

    @NotBlank(message = "환불 사유는 필수입니다.")
    private String cancelReason;
}
