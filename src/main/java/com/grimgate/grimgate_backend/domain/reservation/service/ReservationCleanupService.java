package com.grimgate.grimgate_backend.domain.reservation.service;

import com.grimgate.grimgate_backend.domain.payment.entity.PaymentStatus;
import com.grimgate.grimgate_backend.domain.payment.repository.PaymentRepository;
import com.grimgate.grimgate_backend.domain.reservation.entity.Reservation;
import com.grimgate.grimgate_backend.domain.reservation.entity.ReservationStatus;
import com.grimgate.grimgate_backend.domain.reservation.repository.ReservationRepository;
import com.grimgate.grimgate_backend.domain.theme.entity.TimeSlotStatus;
import com.grimgate.grimgate_backend.domain.theme.repository.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 미결제 예약 및 만료된 슬롯 정리를 트랜잭션 내에서 처리하는 서비스 클래스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationCleanupService {

    private final ReservationRepository reservationRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final PaymentRepository paymentRepository;

    /**
     * 개별 미결제 예약 건을 만료/취소 처리하고 슬롯 및 결제 상태를 복구합니다.
     * 개별 예약별로 독립적인 트랜잭션을 적용하기 위해 호출 측(Scheduler)에서 개별적으로 호출합니다.
     *
     * @param reservationId 정리할 예약 ID
     */
    @Transactional
    public void cleanupReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다. ID: " + reservationId));

        // 이미 결제가 완료되었거나 다른 상태로 변경된 경우 처리하지 않음
        if (reservation.getStatus() != ReservationStatus.PENDING_PAYMENT) {
            log.info("[만료 정리 건너뜀] 예약 ID: {}는 현재 PENDING_PAYMENT 상태가 아님 (현재 상태: {})", 
                    reservationId, reservation.getStatus());
            return;
        }

        // 1. 연결된 결제 정보(Payment)가 존재하는 경우 만료 처리 (PAY_PENDING -> PAYMENT_TIMEOUT)
        paymentRepository.findByReservationId(reservationId).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.PAY_PENDING) {
                payment.timeout();
                log.info("[만료 정리] 결제 ID: {} 상태를 PAYMENT_TIMEOUT으로 변경 완료 (주문번호: {})", 
                        payment.getId(), payment.getOrderId());
            } else {
                log.info("[만료 정리] 결제 ID: {} 가 PAY_PENDING 상태가 아님 (현재 상태: {}). 변경 생략.", 
                        payment.getId(), payment.getStatus());
            }
        });

        // 2. 타임슬롯 상태 복구 (SLOT_HELD -> SLOT_AVAILABLE)
        Long timeSlotId = reservation.getTimeSlot().getId();
        int updatedSlots = timeSlotRepository.updateStatus(
                timeSlotId,
                TimeSlotStatus.SLOT_AVAILABLE,
                TimeSlotStatus.SLOT_HELD,
                LocalDateTime.now()
        );

        if (updatedSlots == 0) {
            log.warn("[만료 정리] 타임슬롯 ID: {} 상태 복구 건너뜀 (현재 슬롯 상태: {})", 
                    timeSlotId, reservation.getTimeSlot().getStatus());
        } else {
            log.info("[만료 정리] 타임슬롯 ID: {} 상태를 SLOT_AVAILABLE로 복구 완료", timeSlotId);
        }

        // 3. 예약 상태 변경 (PENDING_PAYMENT -> CANCELLED)
        reservation.cancel();
        log.info("[만료 정리 완료] 예약 ID: {} 상태를 CANCELLED로 변경 완료 (타임슬롯 ID: {})", 
                reservationId, timeSlotId);
    }
}
