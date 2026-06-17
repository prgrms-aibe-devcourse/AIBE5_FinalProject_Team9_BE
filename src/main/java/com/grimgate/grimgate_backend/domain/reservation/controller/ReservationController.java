package com.grimgate.grimgate_backend.domain.reservation.controller;

import com.grimgate.grimgate_backend.domain.payment.dto.PaymentRefundRequest;
import com.grimgate.grimgate_backend.domain.payment.service.PaymentService;
import com.grimgate.grimgate_backend.domain.reservation.dto.ReservationCancelResponse;
import com.grimgate.grimgate_backend.domain.reservation.dto.ReservationCreateRequest;
import com.grimgate.grimgate_backend.domain.reservation.dto.ReservationCreateResponse;
import com.grimgate.grimgate_backend.domain.reservation.service.ReservationService;
import com.grimgate.grimgate_backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 예약 관련 API를 제공하는 Controller 클래스입니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final PaymentService paymentService;

    /**
     * 예약을 생성하고 PENDING_PAYMENT 상태로 저장합니다.
     *
     * @param request 예약 생성 요청 정보 DTO
     * @return 예약 생성 결과 정보 DTO
     */
    @PostMapping
    public ResponseEntity<ReservationCreateResponse> createReservation(
            @Valid @RequestBody ReservationCreateRequest request
    ) {
        ReservationCreateResponse response = reservationService.createReservation(request);
        return ResponseEntity.ok(response);
    }

    // 예약을 취소합니다.
    // reservationId: 예약 식별자
    // 반환: 예약 취소 결과 응답 DTO
    @PostMapping("/{reservationId}/cancel")
    public ResponseEntity<ApiResponse<ReservationCancelResponse>> cancelReservation(
            @PathVariable Long reservationId
    ) {
        ReservationCancelResponse response = reservationService.cancelReservation(reservationId);

        // CONFIRMED 예약 취소 시 연계된 paymentId가 존재하면 Toss 환불 API를 연계하여 비트랜잭션으로 호출
        if (response.getPaymentId() != null) {
            try {
                paymentService.refundPayment(
                        response.getPaymentId(),
                        PaymentRefundRequest.builder()
                                .cancelReason("사용자 예약 취소로 인한 환불")
                                .build()
                );
            } catch (Exception e) {
                // Toss 환불 API 호출 실패 시 예약 취소는 롤백하지 않고 결제 상태는 PAY_REFUND_PENDING으로 유지
                // 상세 결제 정보(orderId, paymentKey)를 포함한 실패 로그는 PaymentService 내부에서 이미 상세하게 출력하므로,
                // Controller단에서는 연계 실패 여부와 핵심 ID값 위주로 예외 로그를 남깁니다.
                log.error("Toss refund integration failed after reservation cancel - reservationId: {}, paymentId: {}, errorMessage: {}",
                        reservationId, response.getPaymentId(), e.getMessage(), e);
            }
        }

        return ResponseEntity.ok(ApiResponse.success("예약이 취소되었습니다.", response));
    }
}
