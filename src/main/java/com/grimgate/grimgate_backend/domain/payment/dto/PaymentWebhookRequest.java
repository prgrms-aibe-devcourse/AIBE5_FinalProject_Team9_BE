package com.grimgate.grimgate_backend.domain.payment.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

// 토스페이먼츠 웹훅(PAYMENT_STATUS_CHANGED) 데이터를 매핑하기 위한 DTO 클래스입니다.
@Getter
@NoArgsConstructor
@ToString
public class PaymentWebhookRequest {
    private String eventType;
    private String createdAt;
    private WebhookData data;

    // 결제 상태 변경 이벤트의 세부 데이터 객체입니다.
    @Getter
    @NoArgsConstructor
    @ToString
    public static class WebhookData {
        private String mId;
        private String version;
        private String paymentKey;
        private String orderId;
        private String status;
        private String requestedAt;
        private String approvedAt;
        private String lastTransactionKey;
        private String method;
    }
}
