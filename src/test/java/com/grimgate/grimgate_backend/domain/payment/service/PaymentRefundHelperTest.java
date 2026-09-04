package com.grimgate.grimgate_backend.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.grimgate.grimgate_backend.domain.payment.entity.Payment;
import com.grimgate.grimgate_backend.domain.payment.entity.PaymentStatus;
import com.grimgate.grimgate_backend.domain.payment.repository.PaymentRepository;
import com.grimgate.grimgate_backend.global.exception.CustomException;
import com.grimgate.grimgate_backend.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentRefundHelperTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentRefundHelper paymentRefundHelper;

    @Test
    @DisplayName("validateRefund 성공 - 상태가 PAY_REFUND_PENDING이고 paymentKey가 존재하면 정상 반환한다")
    void validateRefund_Success() {
        // given
        Long paymentId = 1L;
        Payment payment = Payment.builder()
                .id(paymentId)
                .status(PaymentStatus.PAY_REFUND_PENDING)
                .paymentKey("toss-key-123")
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        // when
        Payment result = paymentRefundHelper.validateRefund(paymentId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(paymentId);
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PAY_REFUND_PENDING);
    }

    @Test
    @DisplayName("validateRefund 성공 - 멱등성: 상태가 PAY_REFUNDED인 경우 검증을 통과하고 반환한다")
    void validateRefund_AlreadyRefunded_Success() {
        // given
        Long paymentId = 1L;
        Payment payment = Payment.builder()
                .id(paymentId)
                .status(PaymentStatus.PAY_REFUNDED)
                .paymentKey("toss-key-123")
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        // when
        Payment result = paymentRefundHelper.validateRefund(paymentId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PAY_REFUNDED);
    }

    @Test
    @DisplayName("validateRefund 실패 - 결제 건이 존재하지 않으면 PAYMENT_NOT_FOUND 에러를 던진다")
    void validateRefund_NotFound() {
        // given
        Long paymentId = 1L;
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentRefundHelper.validateRefund(paymentId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.PAYMENT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("validateRefund 실패 - 상태가 PAY_REFUND_PENDING 또는 PAY_REFUNDED가 아니면 INVALID_REFUND_STATUS 에러를 던진다")
    void validateRefund_InvalidStatus() {
        // given
        Long paymentId = 1L;
        Payment payment = Payment.builder()
                .id(paymentId)
                .status(PaymentStatus.PAY_SUCCESS) // 결제 성공 상태
                .paymentKey("toss-key-123")
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        // when & then
        assertThatThrownBy(() -> paymentRefundHelper.validateRefund(paymentId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INVALID_REFUND_STATUS.getMessage());
    }

    @Test
    @DisplayName("validateRefund 실패 - paymentKey가 존재하지 않으면 PAYMENT_KEY_MISSING 에러를 던진다")
    void validateRefund_PaymentKeyMissing() {
        // given
        Long paymentId = 1L;
        Payment payment = Payment.builder()
                .id(paymentId)
                .status(PaymentStatus.PAY_REFUND_PENDING)
                .paymentKey(null) // key 누락
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        // when & then
        assertThatThrownBy(() -> paymentRefundHelper.validateRefund(paymentId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.PAYMENT_KEY_MISSING.getMessage());
    }

    @Test
    @DisplayName("saveRefundSuccess 성공 - PAY_REFUND_PENDING 상태인 결제 정보가 정상적으로 환불처리로 업데이트된다")
    void saveRefundSuccess_UpdateSuccess() {
        // given
        Long paymentId = 1L;
        Payment payment = Payment.builder()
                .id(paymentId)
                .status(PaymentStatus.PAY_REFUND_PENDING)
                .amount(22000)
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        LocalDateTime now = LocalDateTime.now();

        // when
        paymentRefundHelper.saveRefundSuccess(paymentId, 22000, now, "고객 변심");

        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAY_REFUNDED);
        assertThat(payment.getRefundAmount()).isEqualTo(22000);
        assertThat(payment.getRefundedAt()).isEqualTo(now);
        assertThat(payment.getCancelReason()).isEqualTo("고객 변심");
    }

    @Test
    @DisplayName("saveRefundSuccess 멱등성 보장 - 이미 PAY_REFUNDED 상태인 경우 무시된다")
    void saveRefundSuccess_AlreadyRefunded_Ignore() {
        // given
        Long paymentId = 1L;
        LocalDateTime originalRefundedAt = LocalDateTime.now().minusDays(1);
        Payment payment = Payment.builder()
                .id(paymentId)
                .status(PaymentStatus.PAY_REFUNDED)
                .amount(22000)
                .refundAmount(22000)
                .refundedAt(originalRefundedAt)
                .cancelReason("이전 사유")
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        // when
        paymentRefundHelper.saveRefundSuccess(paymentId, 22000, LocalDateTime.now(), "새로운 사유");

        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAY_REFUNDED);
        assertThat(payment.getRefundedAt()).isEqualTo(originalRefundedAt);
        assertThat(payment.getCancelReason()).isEqualTo("이전 사유");
    }
}
