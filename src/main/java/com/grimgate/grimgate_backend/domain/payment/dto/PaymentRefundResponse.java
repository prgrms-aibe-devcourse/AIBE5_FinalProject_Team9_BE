package com.grimgate.grimgate_backend.domain.payment.dto;

import com.grimgate.grimgate_backend.domain.payment.entity.Payment;
import com.grimgate.grimgate_backend.domain.payment.entity.PaymentStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentRefundResponse {
    private Long paymentId;
    private String orderId;
    private PaymentStatus status;
    private Integer refundAmount;
    private LocalDateTime refundedAt;
    private String cancelReason;

    public static PaymentRefundResponse of(Payment payment) {
        return PaymentRefundResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .status(payment.getStatus())
                .refundAmount(payment.getRefundAmount())
                .refundedAt(payment.getRefundedAt())
                .cancelReason(payment.getCancelReason())
                .build();
    }
}
