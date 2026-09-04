package com.grimgate.grimgate_backend.domain.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.grimgate.grimgate_backend.domain.payment.entity.Payment;
import com.grimgate.grimgate_backend.domain.payment.entity.PaymentStatus;
import com.grimgate.grimgate_backend.domain.payment.repository.PaymentRepository;
import com.grimgate.grimgate_backend.domain.reservation.entity.Reservation;
import com.grimgate.grimgate_backend.domain.reservation.entity.ReservationStatus;
import com.grimgate.grimgate_backend.domain.reservation.repository.ReservationRepository;
import com.grimgate.grimgate_backend.domain.theme.entity.TimeSlot;
import com.grimgate.grimgate_backend.domain.theme.entity.TimeSlotStatus;
import com.grimgate.grimgate_backend.domain.theme.repository.TimeSlotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ReservationCleanupServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private ReservationCleanupService reservationCleanupService;

    @Test
    @DisplayName("예약 만료 정리 성공 - 결제대기 예약이 취소되고 타임슬롯이 복구되며, 결제가 있을 경우 PAYMENT_TIMEOUT으로 변경된다")
    void cleanupReservation_Success_WithPayment() {
        // given
        Long reservationId = 1L;
        Long timeSlotId = 10L;

        TimeSlot timeSlot = TimeSlot.builder()
                .id(timeSlotId)
                .status(TimeSlotStatus.SLOT_HELD)
                .build();

        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .timeSlot(timeSlot)
                .status(ReservationStatus.PENDING_PAYMENT)
                .build();

        Payment payment = Payment.builder()
                .id(100L)
                .reservation(reservation)
                .status(PaymentStatus.PAY_PENDING)
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(paymentRepository.findByReservationId(reservationId)).thenReturn(Optional.of(payment));
        when(timeSlotRepository.updateStatus(eq(timeSlotId), eq(TimeSlotStatus.SLOT_AVAILABLE), eq(TimeSlotStatus.SLOT_HELD), any(LocalDateTime.class)))
                .thenReturn(1);

        // when
        reservationCleanupService.cleanupReservation(reservationId);

        // then
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAYMENT_TIMEOUT);

        verify(timeSlotRepository).updateStatus(eq(timeSlotId), eq(TimeSlotStatus.SLOT_AVAILABLE), eq(TimeSlotStatus.SLOT_HELD), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("예약 만료 정리 성공 - 결제대기 예약이 취소되고 타임슬롯이 복구되며, 결제 정보가 없을 경우 예약 취소 및 슬롯 복구만 진행된다")
    void cleanupReservation_Success_NoPayment() {
        // given
        Long reservationId = 1L;
        Long timeSlotId = 10L;

        TimeSlot timeSlot = TimeSlot.builder()
                .id(timeSlotId)
                .status(TimeSlotStatus.SLOT_HELD)
                .build();

        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .timeSlot(timeSlot)
                .status(ReservationStatus.PENDING_PAYMENT)
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(paymentRepository.findByReservationId(reservationId)).thenReturn(Optional.empty());
        when(timeSlotRepository.updateStatus(eq(timeSlotId), eq(TimeSlotStatus.SLOT_AVAILABLE), eq(TimeSlotStatus.SLOT_HELD), any(LocalDateTime.class)))
                .thenReturn(1);

        // when
        reservationCleanupService.cleanupReservation(reservationId);

        // then
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);

        verify(timeSlotRepository).updateStatus(eq(timeSlotId), eq(TimeSlotStatus.SLOT_AVAILABLE), eq(TimeSlotStatus.SLOT_HELD), any(LocalDateTime.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("예약 만료 정리 건너뜀 - 이미 PENDING_PAYMENT가 아닌 상태인 경우(예: CONFIRMED), 상태 변경이나 슬롯 복구를 수행하지 않는다")
    void cleanupReservation_Skip_NotPendingPayment() {
        // given
        Long reservationId = 1L;

        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .status(ReservationStatus.CONFIRMED)
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        // when
        reservationCleanupService.cleanupReservation(reservationId);

        // then
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);

        verify(paymentRepository, never()).findByReservationId(anyLong());
        verify(timeSlotRepository, never()).updateStatus(anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("예약 만료 정리 성공 - 타임슬롯 상태 복구 실패(이미 SLOT_AVAILABLE인 경우)에도 예약 및 결제 상태는 정상적으로 만료 처리된다")
    void cleanupReservation_Success_TimeSlotAlreadyAvailable() {
        // given
        Long reservationId = 1L;
        Long timeSlotId = 10L;

        TimeSlot timeSlot = TimeSlot.builder()
                .id(timeSlotId)
                .status(TimeSlotStatus.SLOT_AVAILABLE)
                .build();

        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .timeSlot(timeSlot)
                .status(ReservationStatus.PENDING_PAYMENT)
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(paymentRepository.findByReservationId(reservationId)).thenReturn(Optional.empty());
        // updateStatus가 0을 반환 (이미 SLOT_AVAILABLE이어서 변경할 행이 없음)
        when(timeSlotRepository.updateStatus(eq(timeSlotId), eq(TimeSlotStatus.SLOT_AVAILABLE), eq(TimeSlotStatus.SLOT_HELD), any(LocalDateTime.class)))
                .thenReturn(0);

        // when
        reservationCleanupService.cleanupReservation(reservationId);

        // then
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);

        verify(timeSlotRepository).updateStatus(eq(timeSlotId), eq(TimeSlotStatus.SLOT_AVAILABLE), eq(TimeSlotStatus.SLOT_HELD), any(LocalDateTime.class));
    }
}
