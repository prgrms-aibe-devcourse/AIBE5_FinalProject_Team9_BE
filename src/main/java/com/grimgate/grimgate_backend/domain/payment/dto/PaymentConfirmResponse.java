package com.grimgate.grimgate_backend.domain.payment.dto;

import com.grimgate.grimgate_backend.domain.payment.entity.Payment;
import com.grimgate.grimgate_backend.domain.payment.entity.PaymentStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentConfirmResponse {
    private Long paymentId;
    private Long reservationId;
    private String orderId;
    private Integer amount;
    private PaymentStatus status;
    private String paymentKey;
    private String paymentMethod;
    private LocalDateTime paidAt;

    public static PaymentConfirmResponse of(Payment payment) {
        return PaymentConfirmResponse.builder()
                .paymentId(payment.getId())
                .reservationId(payment.getReservation().getId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paymentKey(payment.getPaymentKey())
                .paymentMethod(payment.getPaymentMethod())
                .paidAt(payment.getPaidAt())
                .build();
    }
}
