package com.grimgate.grimgate_backend.domain.payment.service;

import com.grimgate.grimgate_backend.domain.payment.entity.Payment;
import com.grimgate.grimgate_backend.domain.payment.entity.PaymentStatus;
import com.grimgate.grimgate_backend.domain.payment.repository.PaymentRepository;
import com.grimgate.grimgate_backend.domain.reservation.entity.Reservation;
import com.grimgate.grimgate_backend.domain.theme.entity.TimeSlotStatus;
import com.grimgate.grimgate_backend.domain.theme.repository.TimeSlotRepository;
import com.grimgate.grimgate_backend.global.exception.CustomException;
import com.grimgate.grimgate_backend.global.exception.ErrorCode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PaymentConfirmHelper {

    private final PaymentRepository paymentRepository;
    private final TimeSlotRepository timeSlotRepository;

    /**
     * 결제 승인 완료 처리를 트랜잭션 내에서 처리합니다.
     * 
     * [이슈 #56 비즈니스 요구사항]
     * - Payment 상태 변경 및 paymentKey를 저장합니다.
     * - Reservation 상태를 CONFIRMED로 변경합니다.
     * - TimeSlot 상태를 SLOT_FULL로 변경하여 예약을 확정합니다.
     *
     * @param paymentId 결제 ID
     * @param paymentKey PG사 결제 고유 키
     * @param paymentMethod 결제 수단
     * @param paidAt 결제 시각
     */
    @Transactional
    public void saveConfirmSuccess(Long paymentId, String paymentKey, String paymentMethod, LocalDateTime paidAt) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        // 멱등성 최종 확인 (동시 요청으로 인한 중복 반영 차단)
        if (payment.getStatus() == PaymentStatus.PAY_SUCCESS) {
            return;
        }

        payment.confirm(paymentKey, paymentMethod, paidAt);

        Reservation reservation = payment.getReservation();
        reservation.confirm();

        // 타임슬롯 상태 변경 (SLOT_HELD -> SLOT_FULL)
        timeSlotRepository.updateStatus(
                reservation.getTimeSlot().getId(),
                TimeSlotStatus.SLOT_FULL,
                TimeSlotStatus.SLOT_HELD,
                LocalDateTime.now()
        );
    }

    /**
     * 결제 승인 실패 처리를 트랜잭션 내에서 처리합니다.
     * 
     * [이슈 #56 비즈니스 요구사항]
     * - Payment 상태를 PAY_FAILED로 변경하고 취소 사유를 기록합니다.
     * - Reservation 및 TimeSlot 상태는 기존 상태를 유지하여 스케줄러나 TTL 만료에 맡깁니다.
     *
     * @param paymentId 결제 ID
     * @param errorMessage 에러 및 결제 실패 사유
     */
    @Transactional
    public void saveConfirmFailure(Long paymentId, String errorMessage) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        // 이미 완료된 결제건은 실패 처리하지 않음
        if (payment.getStatus() == PaymentStatus.PAY_SUCCESS) {
            return;
        }

        payment.fail(errorMessage);
    }

    // 결제 성공 웹훅(DONE) 수신 처리를 수행합니다.
    @Transactional
    public void saveWebhookSuccess(Long paymentId, String paymentKey, String paymentMethod, LocalDateTime paidAt) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        // 멱등성 처리: 이미 성공 상태인 경우 아무 작업도 하지 않고 리턴
        if (payment.getStatus() == PaymentStatus.PAY_SUCCESS) {
            return;
        }

        // PAY_PENDING 일 때만 성공 상태로 전이하고 관련 예약/슬롯 처리
        if (payment.getStatus() == PaymentStatus.PAY_PENDING) {
            payment.confirm(paymentKey, paymentMethod, paidAt);

            Reservation reservation = payment.getReservation();
            reservation.confirm();

            // 기존 승인 방식과 동일하게 SLOT_HELD 상태인 경우에만 SLOT_FULL로 변경
            timeSlotRepository.updateStatus(
                    reservation.getTimeSlot().getId(),
                    TimeSlotStatus.SLOT_FULL,
                    TimeSlotStatus.SLOT_HELD,
                    LocalDateTime.now()
            );
        } else {
            // PAY_FAILED, PAYMENT_TIMEOUT 등 이미 최종 상태로 처리된 경우 성공 보정하지 않고 예외 처리
            throw new CustomException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
    }

    // 결제 실패 웹훅(ABORTED) 수신 처리를 수행합니다.
    @Transactional
    public void saveWebhookFailure(Long paymentId, String cancelReason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        // 멱등성 처리: 이미 실패 상태인 경우 아무 작업도 하지 않고 리턴
        if (payment.getStatus() == PaymentStatus.PAY_FAILED) {
            return;
        }

        // 재처리 방지: 이미 성공 완료된 결제는 실패 처리할 수 없음
        if (payment.getStatus() == PaymentStatus.PAY_SUCCESS) {
            throw new CustomException(ErrorCode.INVALID_PAYMENT_STATUS);
        }

        // PAY_PENDING 인 경우에만 실패 상태로 전이
        if (payment.getStatus() == PaymentStatus.PAY_PENDING) {
            payment.fail(cancelReason);
        } else {
            // PAYMENT_TIMEOUT 등 이미 최종 상태로 처리된 경우 예외 처리
            throw new CustomException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
    }

    // 결제 만료 웹훅(EXPIRED) 수신 처리를 수행합니다.
    @Transactional
    public void saveWebhookTimeout(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        // 멱등성 처리: 이미 타임아웃 상태인 경우 아무 작업도 하지 않고 리턴
        if (payment.getStatus() == PaymentStatus.PAYMENT_TIMEOUT) {
            return;
        }

        // 재처리 방지: 이미 성공 완료된 결제는 만료 처리할 수 없음
        if (payment.getStatus() == PaymentStatus.PAY_SUCCESS) {
            throw new CustomException(ErrorCode.INVALID_PAYMENT_STATUS);
        }

        // PAY_PENDING 인 경우에만 만료 상태로 전이
        if (payment.getStatus() == PaymentStatus.PAY_PENDING) {
            payment.timeout();
        } else {
            // PAY_FAILED 등 이미 최종 상태로 처리된 경우 예외 처리
            throw new CustomException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
    }
}
