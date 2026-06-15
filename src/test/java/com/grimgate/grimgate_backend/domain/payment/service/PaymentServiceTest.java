package com.grimgate.grimgate_backend.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.grimgate.grimgate_backend.domain.account.entity.Account;
import com.grimgate.grimgate_backend.domain.member.entity.Member;
import com.grimgate.grimgate_backend.domain.member.repository.MemberRepository;
import com.grimgate.grimgate_backend.domain.payment.dto.PaymentReadyRequest;
import com.grimgate.grimgate_backend.domain.payment.dto.PaymentReadyResponse;
import com.grimgate.grimgate_backend.domain.payment.dto.PaymentConfirmRequest;
import com.grimgate.grimgate_backend.domain.payment.dto.PaymentConfirmResponse;
import com.grimgate.grimgate_backend.domain.payment.dto.PaymentRefundRequest;
import com.grimgate.grimgate_backend.domain.payment.dto.PaymentRefundResponse;
import com.grimgate.grimgate_backend.domain.payment.client.TossPaymentsClient;
import com.grimgate.grimgate_backend.domain.payment.client.TossPaymentsClient.TossConfirmResponseDto;
import com.grimgate.grimgate_backend.domain.payment.client.TossPaymentsClient.TossCancelResponseDto;
import com.grimgate.grimgate_backend.domain.payment.entity.Payment;
import com.grimgate.grimgate_backend.domain.payment.entity.PaymentStatus;
import com.grimgate.grimgate_backend.domain.payment.repository.PaymentRepository;
import com.grimgate.grimgate_backend.domain.reservation.entity.Reservation;
import com.grimgate.grimgate_backend.domain.reservation.entity.ReservationStatus;
import com.grimgate.grimgate_backend.domain.reservation.repository.ReservationRepository;
import com.grimgate.grimgate_backend.domain.theme.entity.Theme;
import com.grimgate.grimgate_backend.domain.theme.entity.TimeSlot;
import com.grimgate.grimgate_backend.global.exception.CustomException;
import com.grimgate.grimgate_backend.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private TossPaymentsClient tossPaymentsClient;

    @Mock
    private PaymentConfirmHelper paymentConfirmHelper;

    @Mock
    private PaymentRefundHelper paymentRefundHelper;

    @InjectMocks
    private PaymentService paymentService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Member setupSecurityContextAndMember(Long accountId, Long memberId, String nickname, String email) {
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        UserDetails userDetails = Mockito.mock(UserDetails.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(String.valueOf(accountId));

        SecurityContextHolder.setContext(securityContext);

        Account account = Account.builder()
                .id(accountId)
                .nickname(nickname)
                .email(email)
                .build();

        Member member = Member.builder()
                .id(memberId)
                .account(account)
                .build();

        when(memberRepository.findByAccount_Id(accountId))
                .thenReturn(Optional.of(member));

        return member;
    }

    @Test
    @DisplayName("결제 준비 성공 - 검증 항목을 모두 통과하면 PAY_PENDING 상태로 결제가 저장된다")
    void readyPayment_Success() {
        // given
        Long accountId = 1L;
        Long memberId = 10L;
        Long reservationId = 100L;
        Integer amount = 22000;

        Member member = setupSecurityContextAndMember(accountId, memberId, "테스터", "test@test.com");

        Theme theme = Theme.builder()
                .title("공포의 방")
                .build();

        TimeSlot timeSlot = TimeSlot.builder()
                .theme(theme)
                .build();

        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .member(member)
                .timeSlot(timeSlot)
                .totalPrice(amount)
                .status(ReservationStatus.PENDING_PAYMENT)
                .build();

        PaymentReadyRequest request = PaymentReadyRequest.builder()
                .reservationId(reservationId)
                .amount(amount)
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(paymentRepository.findByReservationId(reservationId)).thenReturn(Optional.empty());

        Payment mockSavedPayment = Payment.builder()
                .id(1L)
                .reservation(reservation)
                .member(member)
                .amount(amount)
                .orderId("PAY-ORDER-123")
                .status(PaymentStatus.PAY_PENDING)
                .build();
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockSavedPayment);

        // when
        PaymentReadyResponse response = paymentService.readyPayment(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getPaymentId()).isEqualTo(1L);
        assertThat(response.getReservationId()).isEqualTo(reservationId);
        assertThat(response.getOrderId()).isEqualTo("PAY-ORDER-123");
        assertThat(response.getAmount()).isEqualTo(amount);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PAY_PENDING);
        assertThat(response.getOrderName()).isEqualTo("공포의 방");
        assertThat(response.getCustomerName()).isEqualTo("테스터");
        assertThat(response.getCustomerEmail()).isEqualTo("test@test.com");

        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("결제 준비 실패 - 회원을 찾을 수 없으면 MEMBER_NOT_FOUND 에러를 발생시킨다")
    void readyPayment_MemberNotFound() {
        // given
        Long accountId = 1L;
        PaymentReadyRequest request = PaymentReadyRequest.builder()
                .reservationId(100L)
                .amount(22000)
                .build();

        // Security context setup
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        UserDetails userDetails = Mockito.mock(UserDetails.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(String.valueOf(accountId));
        SecurityContextHolder.setContext(securityContext);

        when(memberRepository.findByAccount_Id(accountId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentService.readyPayment(request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("결제 준비 실패 - 예약을 찾을 수 없으면 RESERVATION_NOT_FOUND 에러를 발생시킨다")
    void readyPayment_ReservationNotFound() {
        // given
        Long accountId = 1L;
        Long memberId = 10L;
        Long reservationId = 100L;
        setupSecurityContextAndMember(accountId, memberId, "테스터", "test@test.com");

        PaymentReadyRequest request = PaymentReadyRequest.builder()
                .reservationId(reservationId)
                .amount(22000)
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentService.readyPayment(request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.RESERVATION_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("결제 준비 실패 - 로그인된 회원이 예약의 소유자가 아니면 FORBIDDEN 에러를 발생시킨다")
    void readyPayment_Forbidden() {
        // given
        Long accountId = 1L;
        Long memberId = 10L;
        Long reservationId = 100L;
        setupSecurityContextAndMember(accountId, memberId, "테스터", "test@test.com");

        Member differentMember = Member.builder()
                .id(999L)
                .build();

        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .member(differentMember)
                .build();

        PaymentReadyRequest request = PaymentReadyRequest.builder()
                .reservationId(reservationId)
                .amount(22000)
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> paymentService.readyPayment(request))
                .isInstanceOf(CustomException.class)
                .satisfies(ex -> {
                    CustomException customEx = (CustomException) ex;
                    assertThat(customEx.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    @Test
    @DisplayName("결제 준비 실패 - 예약 상태가 PENDING_PAYMENT가 아니면 INVALID_RESERVATION_STATUS 에러를 발생시킨다")
    void readyPayment_InvalidReservationStatus() {
        // given
        Long accountId = 1L;
        Long memberId = 10L;
        Long reservationId = 100L;
        Member member = setupSecurityContextAndMember(accountId, memberId, "테스터", "test@test.com");

        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .member(member)
                .status(ReservationStatus.CONFIRMED) // 결제 대기가 아님
                .build();

        PaymentReadyRequest request = PaymentReadyRequest.builder()
                .reservationId(reservationId)
                .amount(22000)
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> paymentService.readyPayment(request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INVALID_RESERVATION_STATUS.getMessage());
    }

    @Test
    @DisplayName("결제 준비 실패 - 요청 금액과 예약 총 금액이 일치하지 않으면 PAYMENT_AMOUNT_MISMATCH 에러를 발생시킨다")
    void readyPayment_AmountMismatch() {
        // given
        Long accountId = 1L;
        Long memberId = 10L;
        Long reservationId = 100L;
        Member member = setupSecurityContextAndMember(accountId, memberId, "테스터", "test@test.com");

        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .member(member)
                .totalPrice(22000) // 실제 금액
                .status(ReservationStatus.PENDING_PAYMENT)
                .build();

        PaymentReadyRequest request = PaymentReadyRequest.builder()
                .reservationId(reservationId)
                .amount(33000) // 요청 금액 (위변조 시도)
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> paymentService.readyPayment(request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.PAYMENT_AMOUNT_MISMATCH.getMessage());
    }

    @Test
    @DisplayName("결제 준비 실패 - 이미 해당 예약에 결제 데이터가 존재하면 PAYMENT_ALREADY_EXISTS 에러를 발생시킨다")
    void readyPayment_AlreadyExists() {
        // given
        Long accountId = 1L;
        Long memberId = 10L;
        Long reservationId = 100L;
        Integer amount = 22000;
        Member member = setupSecurityContextAndMember(accountId, memberId, "테스터", "test@test.com");

        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .member(member)
                .totalPrice(amount)
                .status(ReservationStatus.PENDING_PAYMENT)
                .build();

        PaymentReadyRequest request = PaymentReadyRequest.builder()
                .reservationId(reservationId)
                .amount(amount)
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        Payment existingPayment = Payment.builder()
                .id(5L)
                .build();
        when(paymentRepository.findByReservationId(reservationId)).thenReturn(Optional.of(existingPayment));

        // when & then
        assertThatThrownBy(() -> paymentService.readyPayment(request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.PAYMENT_ALREADY_EXISTS.getMessage());
    }

    @Test
    @DisplayName("결제 승인 성공 - 결제대기 건이 정상 승인되면 PAY_SUCCESS 상태의 결제를 반환한다")
    void confirmPayment_Success() {
        // given
        String orderId = "order-123";
        String paymentKey = "toss-key-xyz";
        Integer amount = 22000;

        PaymentConfirmRequest request = PaymentConfirmRequest.builder()
                .orderId(orderId)
                .paymentKey(paymentKey)
                .amount(amount)
                .build();

        Reservation reservation = Reservation.builder()
                .id(100L)
                .build();

        Payment payment = Payment.builder()
                .id(1L)
                .orderId(orderId)
                .amount(amount)
                .status(PaymentStatus.PAY_PENDING)
                .reservation(reservation)
                .build();

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        TossConfirmResponseDto tossResponse = new TossConfirmResponseDto(
                paymentKey, orderId, "카드", "2024-02-13T10:15:30+09:00"
        );
        when(tossPaymentsClient.confirm(paymentKey, orderId, amount)).thenReturn(tossResponse);

        Payment confirmedPayment = Payment.builder()
                .id(1L)
                .orderId(orderId)
                .amount(amount)
                .status(PaymentStatus.PAY_SUCCESS)
                .paymentKey(paymentKey)
                .paymentMethod("카드")
                .paidAt(LocalDateTime.parse("2024-02-13T10:15:30"))
                .reservation(reservation)
                .build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(confirmedPayment));

        // when
        PaymentConfirmResponse response = paymentService.confirmPayment(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getPaymentId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PAY_SUCCESS);
        assertThat(response.getPaymentKey()).isEqualTo(paymentKey);
        assertThat(response.getPaymentMethod()).isEqualTo("카드");

        verify(tossPaymentsClient).confirm(paymentKey, orderId, amount);
        verify(paymentConfirmHelper).saveConfirmSuccess(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(paymentKey), org.mockito.ArgumentMatchers.eq("카드"), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("결제 승인 중복 방지 - 이미 PAY_SUCCESS 상태인 경우 Toss API 호출 없이 성공 응답을 반환한다")
    void confirmPayment_AlreadySuccess() {
        // given
        String orderId = "order-123";
        String paymentKey = "toss-key-xyz";
        Integer amount = 22000;

        PaymentConfirmRequest request = PaymentConfirmRequest.builder()
                .orderId(orderId)
                .paymentKey(paymentKey)
                .amount(amount)
                .build();

        Reservation reservation = Reservation.builder()
                .id(100L)
                .build();

        Payment payment = Payment.builder()
                .id(1L)
                .orderId(orderId)
                .amount(amount)
                .status(PaymentStatus.PAY_SUCCESS)
                .paymentKey(paymentKey)
                .paymentMethod("카드")
                .paidAt(LocalDateTime.now())
                .reservation(reservation)
                .build();

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        // when
        PaymentConfirmResponse response = paymentService.confirmPayment(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getPaymentId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PAY_SUCCESS);

        Mockito.verifyNoInteractions(tossPaymentsClient);
        Mockito.verifyNoInteractions(paymentConfirmHelper);
    }

    @Test
    @DisplayName("결제 승인 실패 - 결제 건을 찾을 수 없는 경우 PAYMENT_NOT_FOUND 에러를 던진다")
    void confirmPayment_NotFound() {
        // given
        PaymentConfirmRequest request = PaymentConfirmRequest.builder()
                .orderId("invalid-order")
                .paymentKey("key")
                .amount(22000)
                .build();

        when(paymentRepository.findByOrderId("invalid-order")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentService.confirmPayment(request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.PAYMENT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("결제 승인 실패 - 결제 건의 상태가 PAY_PENDING이 아니면 INVALID_PAYMENT_STATUS 에러를 던진다")
    void confirmPayment_InvalidStatus() {
        // given
        String orderId = "order-123";
        PaymentConfirmRequest request = PaymentConfirmRequest.builder()
                .orderId(orderId)
                .paymentKey("key")
                .amount(22000)
                .build();

        Payment payment = Payment.builder()
                .id(1L)
                .orderId(orderId)
                .amount(22000)
                .status(PaymentStatus.PAY_FAILED)
                .build();

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        // when & then
        assertThatThrownBy(() -> paymentService.confirmPayment(request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.INVALID_PAYMENT_STATUS.getMessage());
    }

    @Test
    @DisplayName("결제 승인 실패 - 요청 결제 금액과 결제 건의 금액이 일치하지 않으면 PAYMENT_AMOUNT_MISMATCH 에러를 던진다")
    void confirmPayment_AmountMismatch() {
        // given
        String orderId = "order-123";
        PaymentConfirmRequest request = PaymentConfirmRequest.builder()
                .orderId(orderId)
                .paymentKey("key")
                .amount(33000)
                .build();

        Payment payment = Payment.builder()
                .id(1L)
                .orderId(orderId)
                .amount(22000)
                .status(PaymentStatus.PAY_PENDING)
                .build();

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        // when & then
        assertThatThrownBy(() -> paymentService.confirmPayment(request))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.PAYMENT_AMOUNT_MISMATCH.getMessage());
    }

    @Test
    @DisplayName("결제 승인 실패 - 토스 API가 에러를 반환하면 saveConfirmFailure가 실행되며 CustomException이 던져진다")
    void confirmPayment_TossFailure() {
        // given
        String orderId = "order-123";
        String paymentKey = "toss-key-xyz";
        Integer amount = 22000;

        PaymentConfirmRequest request = PaymentConfirmRequest.builder()
                .orderId(orderId)
                .paymentKey(paymentKey)
                .amount(amount)
                .build();

        Payment payment = Payment.builder()
                .id(1L)
                .orderId(orderId)
                .amount(amount)
                .status(PaymentStatus.PAY_PENDING)
                .build();

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        when(tossPaymentsClient.confirm(paymentKey, orderId, amount))
                .thenThrow(new RuntimeException("PG 승인 한도 초과"));

        // when & then
        assertThatThrownBy(() -> paymentService.confirmPayment(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("결제 승인 과정에서 오류가 발생했습니다: PG 승인 한도 초과");

        verify(paymentConfirmHelper).saveConfirmFailure(1L, "PG 승인 한도 초과");
    }

    // 올바른 테스트 서명을 생성하는 헬퍼 메서드입니다.
    private String generateTestSignature(String payload, String transmissionTime, String secretKey) throws Exception {
        String message = payload + ":" + transmissionTime;
        javax.crypto.Mac sha256HMAC = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(
                secretKey.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        sha256HMAC.init(secretKeySpec);
        byte[] hashBytes = sha256HMAC.doFinal(message.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return "v1:" + java.util.Base64.getEncoder().encodeToString(hashBytes);
    }

    @Test
    @DisplayName("웹훅 성공 - DONE 상태의 웹훅 수신 시 saveWebhookSuccess를 정상적으로 호출한다")
    void processWebhook_Success_Done() throws Exception {
        // given
        String secretKey = "test_webhook_secret_key";
        org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "webhookSecretKey", secretKey);
        org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "objectMapper", new com.fasterxml.jackson.databind.ObjectMapper());

        String orderId = "order-123";
        String payload = "{"
                + "\"eventType\":\"PAYMENT_STATUS_CHANGED\","
                + "\"createdAt\":\"2026-06-10T12:00:00.000000\","
                + "\"data\":{"
                + "\"orderId\":\"" + orderId + "\","
                + "\"paymentKey\":\"toss-key-xyz\","
                + "\"status\":\"DONE\","
                + "\"method\":\"카드\","
                + "\"approvedAt\":\"2026-06-10T12:00:05+09:00\""
                + "}"
                + "}";
        String transmissionTime = "2026-06-10T12:00:10+09:00";
        String signature = generateTestSignature(payload, transmissionTime, secretKey);

        Payment payment = Payment.builder()
                .id(1L)
                .orderId(orderId)
                .status(PaymentStatus.PAY_PENDING)
                .build();

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        // when
        paymentService.processWebhook(payload, signature, transmissionTime);

        // then
        verify(paymentConfirmHelper).saveWebhookSuccess(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("toss-key-xyz"),
                org.mockito.ArgumentMatchers.eq("카드"),
                any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("웹훅 성공 - ABORTED 상태의 웹훅 수신 시 saveWebhookFailure를 정상적으로 호출한다")
    void processWebhook_Success_Aborted() throws Exception {
        // given
        String secretKey = "test_webhook_secret_key";
        org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "webhookSecretKey", secretKey);
        org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "objectMapper", new com.fasterxml.jackson.databind.ObjectMapper());

        String orderId = "order-123";
        String payload = "{"
                + "\"eventType\":\"PAYMENT_STATUS_CHANGED\","
                + "\"createdAt\":\"2026-06-10T12:00:00.000000\","
                + "\"data\":{"
                + "\"orderId\":\"" + orderId + "\","
                + "\"status\":\"ABORTED\""
                + "}"
                + "}";
        String transmissionTime = "2026-06-10T12:00:10+09:00";
        String signature = generateTestSignature(payload, transmissionTime, secretKey);

        Payment payment = Payment.builder()
                .id(1L)
                .orderId(orderId)
                .status(PaymentStatus.PAY_PENDING)
                .build();

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        // when
        paymentService.processWebhook(payload, signature, transmissionTime);

        // then
        verify(paymentConfirmHelper).saveWebhookFailure(
                org.mockito.ArgumentMatchers.eq(1L),
                any(String.class)
        );
    }

    @Test
    @DisplayName("웹훅 성공 - EXPIRED 상태의 웹훅 수신 시 saveWebhookTimeout을 정상적으로 호출한다")
    void processWebhook_Success_Expired() throws Exception {
        // given
        String secretKey = "test_webhook_secret_key";
        org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "webhookSecretKey", secretKey);
        org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "objectMapper", new com.fasterxml.jackson.databind.ObjectMapper());

        String orderId = "order-123";
        String payload = "{"
                + "\"eventType\":\"PAYMENT_STATUS_CHANGED\","
                + "\"createdAt\":\"2026-06-10T12:00:00.000000\","
                + "\"data\":{"
                + "\"orderId\":\"" + orderId + "\","
                + "\"status\":\"EXPIRED\""
                + "}"
                + "}";
        String transmissionTime = "2026-06-10T12:00:10+09:00";
        String signature = generateTestSignature(payload, transmissionTime, secretKey);

        Payment payment = Payment.builder()
                .id(1L)
                .orderId(orderId)
                .status(PaymentStatus.PAY_PENDING)
                .build();

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        // when
        paymentService.processWebhook(payload, signature, transmissionTime);

        // then
        verify(paymentConfirmHelper).saveWebhookTimeout(
                org.mockito.ArgumentMatchers.eq(1L)
        );
    }

    @Test
    @DisplayName("웹훅 실패 - 서명이 올바르지 않으면 WEBHOOK_VERIFICATION_FAILED 에러를 던진다")
    void processWebhook_Fail_SignatureMismatch() throws Exception {
        // given
        String secretKey = "test_webhook_secret_key";
        org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "webhookSecretKey", secretKey);
        org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "objectMapper", new com.fasterxml.jackson.databind.ObjectMapper());

        String payload = "{\"eventType\":\"PAYMENT_STATUS_CHANGED\"}";
        String transmissionTime = "2026-06-10T12:00:10+09:00";
        String invalidSignature = "v1:invalid-sig-data";

        // when & then
        assertThatThrownBy(() -> paymentService.processWebhook(payload, invalidSignature, transmissionTime))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.WEBHOOK_VERIFICATION_FAILED.getMessage());
    }

    @Test
    @DisplayName("웹훅 실패 - 서명 헤더나 전송시간 헤더가 없으면 WEBHOOK_VERIFICATION_FAILED 에러를 던진다")
    void processWebhook_Fail_MissingHeaders() {
        // given
        String payload = "{\"eventType\":\"PAYMENT_STATUS_CHANGED\"}";

        // when & then
        assertThatThrownBy(() -> paymentService.processWebhook(payload, null, null))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.WEBHOOK_VERIFICATION_FAILED.getMessage());
    }

    @Test
    @DisplayName("결제 환불 성공 - 정상적인 환불 대기 건의 환불이 진행되면 PAY_REFUNDED 상태가 반환된다")
    void refundPayment_Success() {
        // given
        Long paymentId = 1L;
        String paymentKey = "toss-key-xyz";
        String orderId = "order-uuid-123";
        String cancelReason = "고객 변심";
        Integer amount = 22000;

        PaymentRefundRequest request = PaymentRefundRequest.builder()
                .cancelReason(cancelReason)
                .build();

        Payment paymentBefore = Payment.builder()
                .id(paymentId)
                .paymentKey(paymentKey)
                .orderId(orderId)
                .amount(amount)
                .status(PaymentStatus.PAY_REFUND_PENDING)
                .build();

        when(paymentRefundHelper.validateRefund(paymentId)).thenReturn(paymentBefore);

        TossCancelResponseDto tossResponse = new TossCancelResponseDto(
                paymentKey,
                orderId,
                "CANCELED",
                java.util.List.of(new TossCancelResponseDto.TossCancelDetail(
                        amount,
                        cancelReason,
                        "2026-06-15T11:00:00+09:00"
                ))
        );
        when(tossPaymentsClient.cancel(paymentKey, cancelReason)).thenReturn(tossResponse);

        Payment paymentAfter = Payment.builder()
                .id(paymentId)
                .paymentKey(paymentKey)
                .orderId(orderId)
                .amount(amount)
                .refundAmount(amount)
                .refundedAt(LocalDateTime.parse("2026-06-15T11:00:00"))
                .cancelReason(cancelReason)
                .status(PaymentStatus.PAY_REFUNDED)
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(paymentAfter));

        // when
        PaymentRefundResponse response = paymentService.refundPayment(paymentId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getPaymentId()).isEqualTo(paymentId);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PAY_REFUNDED);
        assertThat(response.getRefundAmount()).isEqualTo(amount);
        assertThat(response.getCancelReason()).isEqualTo(cancelReason);

        verify(paymentRefundHelper).validateRefund(paymentId);
        verify(tossPaymentsClient).cancel(paymentKey, cancelReason);
        verify(paymentRefundHelper).saveRefundSuccess(
                org.mockito.ArgumentMatchers.eq(paymentId),
                org.mockito.ArgumentMatchers.eq(amount),
                any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.eq(cancelReason)
        );
    }

    @Test
    @DisplayName("결제 환불 멱등성 보장 - 이미 PAY_REFUNDED인 상태일 때 Toss API 호출 없이 성공 결과를 즉시 반환한다")
    void refundPayment_AlreadyRefunded() {
        // given
        Long paymentId = 1L;
        String paymentKey = "toss-key-xyz";
        String orderId = "order-uuid-123";
        String cancelReason = "고객 변심";
        Integer amount = 22000;

        PaymentRefundRequest request = PaymentRefundRequest.builder()
                .cancelReason(cancelReason)
                .build();

        Payment payment = Payment.builder()
                .id(paymentId)
                .paymentKey(paymentKey)
                .orderId(orderId)
                .amount(amount)
                .refundAmount(amount)
                .refundedAt(LocalDateTime.now())
                .cancelReason(cancelReason)
                .status(PaymentStatus.PAY_REFUNDED)
                .build();

        when(paymentRefundHelper.validateRefund(paymentId)).thenReturn(payment);

        // when
        PaymentRefundResponse response = paymentService.refundPayment(paymentId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getPaymentId()).isEqualTo(paymentId);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PAY_REFUNDED);

        Mockito.verifyNoInteractions(tossPaymentsClient);
        Mockito.verifyNoMoreInteractions(paymentRefundHelper);
    }

    @Test
    @DisplayName("결제 환불 실패 - Toss API에서 4xx 오류 반환 시 CustomException을 던지며 상태변경 저장을 수행하지 않는다")
    void refundPayment_TossClientError() {
        // given
        Long paymentId = 1L;
        String paymentKey = "toss-key-xyz";
        String orderId = "order-uuid-123";
        String cancelReason = "고객 변심";

        PaymentRefundRequest request = PaymentRefundRequest.builder()
                .cancelReason(cancelReason)
                .build();

        Payment payment = Payment.builder()
                .id(paymentId)
                .paymentKey(paymentKey)
                .orderId(orderId)
                .amount(22000)
                .status(PaymentStatus.PAY_REFUND_PENDING)
                .build();

        when(paymentRefundHelper.validateRefund(paymentId)).thenReturn(payment);

        byte[] bodyBytes = "{\"code\":\"ALREADY_CANCELED_PAYMENT\",\"message\":\"이미 취소된 결제입니다.\"}".getBytes();
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        
        org.springframework.web.reactive.function.client.WebClientResponseException mockException = 
                new org.springframework.web.reactive.function.client.WebClientResponseException(
                        400, "Bad Request", headers, bodyBytes, java.nio.charset.StandardCharsets.UTF_8
                );

        when(tossPaymentsClient.cancel(paymentKey, cancelReason)).thenThrow(mockException);

        // when & then
        assertThatThrownBy(() -> paymentService.refundPayment(paymentId, request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("토스 환불 API 호출 중 에러가 발생했습니다");

        verify(paymentRefundHelper).validateRefund(paymentId);
        verify(tossPaymentsClient).cancel(paymentKey, cancelReason);
        Mockito.verifyNoMoreInteractions(paymentRefundHelper); // saveRefundSuccess가 호출되지 않아야 함
    }
}
