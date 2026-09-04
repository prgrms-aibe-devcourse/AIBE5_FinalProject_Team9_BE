package com.grimgate.grimgate_backend.domain.payment.repository;

import com.grimgate.grimgate_backend.domain.payment.entity.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByReservationId(Long reservationId);
    Optional<Payment> findByOrderId(String orderId);
}
