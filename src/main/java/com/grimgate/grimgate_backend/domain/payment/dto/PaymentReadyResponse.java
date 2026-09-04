package com.grimgate.grimgate_backend.domain.payment.dto;

import com.grimgate.grimgate_backend.domain.payment.entity.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentReadyResponse {
    private Long paymentId;
    private Long reservationId;
    private String orderId;
    private Integer amount;
    private PaymentStatus status;
    private String orderName;
    private String customerName;
    private String customerEmail;
}
