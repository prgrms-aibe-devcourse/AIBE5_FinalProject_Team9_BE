package com.grimgate.grimgate_backend.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PaymentConfirmRequest {

    @NotBlank(message = "결제 고유 키(paymentKey)는 필수입니다.")
    private String paymentKey;

    @NotBlank(message = "주문 식별자(orderId)는 필수입니다.")
    private String orderId;

    @NotNull(message = "결제 금액은 필수입니다.")
    @Positive(message = "결제 금액은 0보다 커야 합니다.")
    private Integer amount;
}
