package com.grimgate.grimgate_backend.domain.reservation.scheduler;

import com.grimgate.grimgate_backend.domain.reservation.entity.Reservation;
import com.grimgate.grimgate_backend.domain.reservation.entity.ReservationStatus;
import com.grimgate.grimgate_backend.domain.reservation.repository.ReservationRepository;
import com.grimgate.grimgate_backend.domain.reservation.service.ReservationCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 결제 대기 상태로 장시간 방치된 예약을 주기적으로 감지하고 정리하는 스케줄러 클래스입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationCleanupScheduler {

    private final ReservationRepository reservationRepository;
    private final ReservationCleanupService reservationCleanupService;

    // 만료 시간 설정 (기본값: 15분)
    @Value("${reservation.cleanup.expiration-minutes:15}")
    private int expirationMinutes;

    /**
     * 주기적으로 미결제 만료 예약을 감지하여 정리 메서드를 호출합니다.
     * 기본 스케줄러 실행 주기는 매 1분(0 * * * * *)입니다.
     * application.yml 에서 reservation.cleanup.cron 속성으로 주기를 재정의할 수 있습니다.
     */
    @Scheduled(cron = "${reservation.cleanup.cron:0 * * * * *}")
    public void cleanupExpiredReservations() {
        LocalDateTime timeLimit = LocalDateTime.now().minusMinutes(expirationMinutes);
        
        // PENDING_PAYMENT 상태이며, 생성 시각이 15분(expirationMinutes)을 초과한 예약 목록 조회
        List<Reservation> expiredReservations = reservationRepository.findExpiredReservations(
                ReservationStatus.PENDING_PAYMENT,
                timeLimit
        );

        if (expiredReservations.isEmpty()) {
            return;
        }

        log.info("[만료 예약 정리 스케줄러 시작] 대상 건수: {}건 (만료 기준 시간: {})", 
                expiredReservations.size(), timeLimit);

        int processedCount = 0;
        int failedCount = 0;

        for (Reservation reservation : expiredReservations) {
            try {
                // 개별 예약을 독립 트랜잭션 내에서 정리 처리
                reservationCleanupService.cleanupReservation(reservation.getId());
                processedCount++;
            } catch (Exception e) {
                failedCount++;
                log.error("[만료 예약 정리 실패] 예약 ID: {}, 에러: {}", 
                        reservation.getId(), e.getMessage(), e);
            }
        }

        log.info("[만료 예약 정리 스케줄러 종료] 처리 완료: {}건, 처리 실패: {}건", 
                processedCount, failedCount);
    }
}
