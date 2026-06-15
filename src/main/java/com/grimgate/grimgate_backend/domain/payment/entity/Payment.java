package com.grimgate.grimgate_backend.domain.payment.entity;

import com.grimgate.grimgate_backend.domain.member.entity.Member;
import com.grimgate.grimgate_backend.domain.reservation.entity.Reservation;
import com.grimgate.grimgate_backend.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 정보를 저장하고 관리하는 JPA 엔티티입니다.
 * 
 * [이슈 #48 구현 범위 주의사항]
 * - 본 이슈에서는 결제 준비(ready) 단계까지만 저장합니다.
 * - 결제 승인(confirm), 웹훅(webhook), 취소 및 환불(cancel)에 대한 비즈니스 로직 및 관련 필드 업데이트는 후속 이슈 범위입니다.
 */
@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 결제 대상 예약 정보입니다.
     * 한 예약(Reservation)은 하나의 결제(Payment) 시도/내역만 가질 수 있도록 1:1 관계(Unique 제약 조건)를 맺습니다.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservation;

    /**
     * 결제를 진행하는 회원(Member) 정보입니다.
     * 통계 조회나 정산, 회원별 결제 이력 조회를 빠르게 하기 위해 결제 테이블에 직접 저장합니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /**
     * 결제 금액입니다. 위변조 검증 시 예약 금액과 일치하는지 비교하는 기준이 됩니다.
     */
    @Column(name = "amount", nullable = false)
    private Integer amount;

    /**
     * 결제 수단 (예: 카드, 간편결제, 계좌이체 등)
     * 결제 준비(ready) 단계에서는 null이며, 결제 완료(confirm) 단계에서 채워집니다.
     */
    @Column(name = "payment_method")
    private String paymentMethod;

    /**
     * PG사(토스 페이먼츠)에서 발급하는 결제 건에 대한 고유 키값입니다.
     * 결제 준비(ready) 단계에서는 발급되지 않아 null이며, 최종 결제 승인(confirm) 및 환불(cancel) 시 인증키로 사용됩니다.
     */
    @Column(name = "payment_key")
    private String paymentKey;

    /**
     * 주문 ID입니다. 우리 시스템에서 자체적으로 생성하는 결제 건의 고유 식별자(UUID)입니다.
     * 토스 페이먼츠 결제창에 이 값을 전달하여 결제를 요청하게 됩니다.
     */
    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    /**
     * 결제 진행 상태 (초기값: PAY_PENDING)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    /**
     * 최종 결제 승인이 완료된 시각
     */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /**
     * 예약 취소 등으로 환불 처리된 금액 (부분/전액 환불)
     */
    @Column(name = "refund_amount")
    private Integer refundAmount;

    /**
     * 환불 처리가 완료된 시각
     */
    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    /**
     * 결제 실패 또는 환불(취소) 시의 사유
     */
    @Column(name = "cancel_reason")
    private String cancelReason;

    /**
     * 결제 승인 완료 처리를 수행합니다.
     * 결제 상태를 PAY_SUCCESS로 변경하고 결제 승인 관련 정보를 저장합니다.
     *
     * @param paymentKey PG사 결제 고유 키값
     * @param paymentMethod 결제 수단
     * @param paidAt 결제 승인 시각
     */
    public void confirm(String paymentKey, String paymentMethod, LocalDateTime paidAt) {
        this.paymentKey = paymentKey;
        this.paymentMethod = paymentMethod;
        this.status = PaymentStatus.PAY_SUCCESS;
        this.paidAt = paidAt;
    }

    /**
     * 결제 승인 실패 처리를 수행합니다.
     * 결제 상태를 PAY_FAILED로 변경하고 실패 사유를 기록합니다.
     *
     * @param cancelReason 결제 실패 사유
     */
    public void fail(String cancelReason) {
        this.status = PaymentStatus.PAY_FAILED;
        this.cancelReason = cancelReason;
    }

    // 결제 성공 후 예약 취소 시 실제 환불 전까지 구분하기 위해 상태를 PAY_REFUND_PENDING으로 변경하고 취소 사유를 기록합니다.
    public void refundPending(String cancelReason) {
        this.status = PaymentStatus.PAY_REFUND_PENDING;
        this.cancelReason = cancelReason;
    }

    // 환불 처리가 완료되었을 때 상태를 PAY_REFUNDED로 변경하고 환불 세부정보를 저장합니다.
    public void refundSuccess(Integer refundAmount, LocalDateTime refundedAt, String cancelReason) {
        this.status = PaymentStatus.PAY_REFUNDED;
        this.refundAmount = refundAmount;
        this.refundedAt = refundedAt;
        this.cancelReason = cancelReason;
    }

    // 결제 시간 초과(timeout) 처리를 수행하며 상태를 PAYMENT_TIMEOUT으로 변경합니다.
    public void timeout() {
        this.status = PaymentStatus.PAYMENT_TIMEOUT;
    }
}
