package com.grimgate.grimgate_backend.domain.payment.service;

import com.grimgate.grimgate_backend.domain.payment.entity.Payment;
import com.grimgate.grimgate_backend.domain.payment.entity.PaymentStatus;
import com.grimgate.grimgate_backend.domain.payment.repository.PaymentRepository;
import com.grimgate.grimgate_backend.global.exception.CustomException;
import com.grimgate.grimgate_backend.global.exception.ErrorCode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PaymentRefundHelper {

    private final PaymentRepository paymentRepository;

    /**
     * 환불 요청에 대해 사전 검증을 수행합니다. (읽기 전용 트랜잭션)
     * 이미 환불 완료된 건은 멱등 처리를 위해 그대로 반환합니다.
     *
     * @param paymentId 결제 ID
     * @return 검증이 완료된 Payment 엔티티
     */
    @Transactional(readOnly = true)
    public Payment validateRefund(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        // 멱등성 처리: 이미 환불이 완료된 상태라면 Toss API를 타지 않고 반환할 수 있도록 통과시킵니다.
        if (payment.getStatus() == PaymentStatus.PAY_REFUNDED) {
            return payment;
        }

        // 환불 대기 상태(PAY_REFUND_PENDING)인지 검증합니다.
        if (payment.getStatus() != PaymentStatus.PAY_REFUND_PENDING) {
            throw new CustomException(ErrorCode.INVALID_REFUND_STATUS);
        }

        // 토스 결제 취소를 위한 paymentKey 존재 여부 검증
        if (payment.getPaymentKey() == null || payment.getPaymentKey().isBlank()) {
            throw new CustomException(ErrorCode.PAYMENT_KEY_MISSING);
        }

        return payment;
    }

    /**
     * 환불 성공 처리 및 상태 저장을 수행합니다. (쓰기 트랜잭션)
     *
     * @param paymentId 결제 ID
     * @param refundAmount 환불된 금액
     * @param refundedAt 환불 처리 완료 시각
     * @param cancelReason 환불 사유
     */
    @Transactional
    public void saveRefundSuccess(Long paymentId, Integer refundAmount, LocalDateTime refundedAt, String cancelReason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        // 멱등성 최종 처리: 동시 요청으로 이미 환불 완료 처리되었는지 확인
        if (payment.getStatus() == PaymentStatus.PAY_REFUNDED) {
            return;
        }

        payment.refundSuccess(refundAmount, refundedAt, cancelReason);
    }
}
