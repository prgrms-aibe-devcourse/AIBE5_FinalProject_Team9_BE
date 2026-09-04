package com.grimgate.grimgate_backend.domain.payment.controller;

import com.grimgate.grimgate_backend.domain.payment.dto.PaymentConfirmRequest;
import com.grimgate.grimgate_backend.domain.payment.dto.PaymentConfirmResponse;
import com.grimgate.grimgate_backend.domain.payment.dto.PaymentReadyRequest;
import com.grimgate.grimgate_backend.domain.payment.dto.PaymentReadyResponse;
import com.grimgate.grimgate_backend.domain.payment.dto.PaymentRefundRequest;
import com.grimgate.grimgate_backend.domain.payment.dto.PaymentRefundResponse;
import com.grimgate.grimgate_backend.domain.payment.service.PaymentService;
import com.grimgate.grimgate_backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 결제 준비 API를 호출하여 결제 데이터를 생성하고 초기 상태를 설정합니다.
     *
     * @param request 결제 준비 요청 정보 DTO
     * @return 결제 준비 결과 정보 DTO (Toss widget 렌더링에 필요한 정보 포함)
     */
    @PostMapping("/ready")
    public ResponseEntity<ApiResponse<PaymentReadyResponse>> readyPayment(
            @Valid @RequestBody PaymentReadyRequest request
    ) {
        PaymentReadyResponse response = paymentService.readyPayment(request);
        return ResponseEntity.ok(ApiResponse.success("결제 준비가 완료되었습니다.", response));
    }

    /**
     * 결제 승인 API
     * 토스페이먼츠 승인 요청 결과를 처리하여 최종 예약 확정 및 슬롯 마감 처리를 완료합니다.
     *
     * @param request 결제 승인 요청 DTO
     * @return 결제 승인 완료 결과 응답 DTO
     */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PaymentConfirmResponse>> confirmPayment(
            @Valid @RequestBody PaymentConfirmRequest request
    ) {
        PaymentConfirmResponse response = paymentService.confirmPayment(request);
        return ResponseEntity.ok(ApiResponse.success("결제 승인이 완료되었습니다.", response));
    }

    // 토스페이먼츠 웹훅 요청을 수신하여 결제 상태를 동기화합니다.
    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "tosspayments-webhook-signature", required = false) String signature,
            @RequestHeader(value = "tosspayments-webhook-transmission-time", required = false) String transmissionTime
    ) {
        paymentService.processWebhook(payload, signature, transmissionTime);
        return ResponseEntity.ok().build();
    }

    /**
     * 결제 환불 API
     * 토스페이먼츠 환불 요청을 처리합니다.
     *
     * @param paymentId 결제 ID
     * @param request 환불 요청 DTO
     * @return 결제 환불 완료 결과 응답 DTO
     */
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<ApiResponse<PaymentRefundResponse>> refundPayment(
            @PathVariable Long paymentId,
            @Valid @RequestBody PaymentRefundRequest request
    ) {
        PaymentRefundResponse response = paymentService.refundPayment(paymentId, request);
        return ResponseEntity.ok(ApiResponse.success("환불 처리가 완료되었습니다.", response));
    }
}
