package com.grimgate.grimgate_backend.domain.payment.service;

import com.grimgate.grimgate_backend.domain.member.entity.Member;
import com.grimgate.grimgate_backend.domain.member.repository.MemberRepository;
import com.grimgate.grimgate_backend.domain.payment.dto.PaymentReadyRequest;
import com.grimgate.grimgate_backend.domain.payment.dto.PaymentReadyResponse;
import com.grimgate.grimgate_backend.domain.payment.entity.Payment;
import com.grimgate.grimgate_backend.domain.payment.entity.PaymentStatus;
import com.grimgate.grimgate_backend.domain.payment.repository.PaymentRepository;
import com.grimgate.grimgate_backend.domain.payment.client.TossPaymentsClient;
import com.grimgate.grimgate_backend.domain.payment.client.TossPaymentsClient.TossConfirmResponseDto;
import com.grimgate.grimgate_backend.domain.payment.client.TossPaymentsClient.TossCancelResponseDto;
import com.grimgate.grimgate_backend.domain.payment.dto.PaymentConfirmRequest;
import com.grimgate.grimgate_backend.domain.payment.dto.PaymentConfirmResponse;
import com.grimgate.grimgate_backend.domain.payment.dto.PaymentRefundRequest;
import com.grimgate.grimgate_backend.domain.payment.dto.PaymentRefundResponse;
import com.grimgate.grimgate_backend.domain.reservation.entity.Reservation;
import com.grimgate.grimgate_backend.domain.reservation.entity.ReservationStatus;
import com.grimgate.grimgate_backend.domain.reservation.repository.ReservationRepository;
import com.grimgate.grimgate_backend.global.exception.CustomException;
import com.grimgate.grimgate_backend.global.exception.ErrorCode;
import com.grimgate.grimgate_backend.global.security.SecurityUtil;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grimgate.grimgate_backend.domain.payment.dto.PaymentWebhookRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final TossPaymentsClient tossPaymentsClient;
    private final PaymentConfirmHelper paymentConfirmHelper;
    private final PaymentRefundHelper paymentRefundHelper;
    private final ObjectMapper objectMapper;

    @Value("${toss.webhook-secret-key:dummy_webhook_secret_key}")
    private String webhookSecretKey;

    /**
     * 결제 준비(Ready) 단계를 처리합니다.
     * 
     * [이슈 #48 비즈니스 요구사항]
     * - Toss 결제창 요청 전, 금액 위변조 및 중복 결제 요청 방지를 위한 검증을 수행합니다.
     * - 결제 최종 승인(confirm), 웹훅(webhook), 결제 실패/환불(cancel) 처리는 본 이슈 범위에서 제외하며 후속 이슈로 처리합니다.
     *
     * @param request 결제 준비 요청 DTO
     * @return 결제 준비 결과 응답 DTO (토스 위젯 렌더링에 필요한 정보 포함)
     */
    @Transactional
    public PaymentReadyResponse readyPayment(PaymentReadyRequest request) {
        // [검증 흐름 순서]

        // 1) 로그인 사용자 조회
        Long accountId = SecurityUtil.getCurrentAccountId();
        Member member = memberRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 2) 예약 존재 여부 확인
        Reservation reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESERVATION_NOT_FOUND));

        // 3) 예약자 본인 여부 확인 (예약의 소유권 검증)
        if (!reservation.getMember().getId().equals(member.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // 4) 예약 상태 검증 (결제 대기인 PENDING_PAYMENT 상태일 때만 결제 진행 허용)
        if (reservation.getStatus() != ReservationStatus.PENDING_PAYMENT) {
            throw new CustomException(ErrorCode.INVALID_RESERVATION_STATUS);
        }

        // 5) 결제 금액 위변조 검증 (프론트에서 전달된 결제 요청 금액과 실제 DB의 예약 총 금액 일치 여부 확인)
        if (!reservation.getTotalPrice().equals(request.getAmount())) {
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // 6) 중복 결제 검증 (Reservation과 Payment는 1:1 관계이므로, 이미 해당 예약에 대한 결제 내역이 존재하는지 확인)
        if (paymentRepository.findByReservationId(request.getReservationId()).isPresent()) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_EXISTS);
        }

        // 7) orderId 생성 및 PAY_PENDING 저장
        // - PG사 결제 연동에 사용할 유니크한 주문 ID를 UUID 기반으로 생성합니다.
        String orderId = UUID.randomUUID().toString();

        // - 초기 결제 상태를 PAY_PENDING(결제 대기)으로 설정하여 결제 데이터를 데이터베이스에 생성 및 저장합니다.
        Payment payment = Payment.builder()
                .reservation(reservation)
                .member(member)
                .amount(request.getAmount())
                .orderId(orderId)
                .status(PaymentStatus.PAY_PENDING)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // 8. 토스 결제 위젯 렌더링에 필요한 추가 메타데이터 조회
        String orderName = reservation.getTimeSlot().getTheme().getTitle();
        String customerName = member.getAccount().getNickname();
        String customerEmail = member.getAccount().getEmail();

        // 9. 응답 DTO 반환
        return PaymentReadyResponse.builder()
                .paymentId(savedPayment.getId())
                .reservationId(reservation.getId())
                .orderId(savedPayment.getOrderId())
                .amount(savedPayment.getAmount())
                .status(savedPayment.getStatus())
                .orderName(orderName)
                .customerName(customerName)
                .customerEmail(customerEmail)
                .build();
    }

    /**
     * 결제 승인(Confirm) 단계를 처리합니다.
     * 외부 PG API 호출로 인한 DB 커넥션 풀 고갈을 방지하고자 트랜잭션 없이 시작하여 내부 전이 메서드들을 호출합니다.
     *
     * @param request 결제 승인 요청 DTO
     * @return 결제 승인 완료 결과 응답 DTO
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PaymentConfirmResponse confirmPayment(PaymentConfirmRequest request) {
        // 1. orderId 기준 Payment 조회
        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        // 2. 중복 승인 방지 및 멱등성 보장 (이미 PAY_SUCCESS 상태라면 Toss API를 타지 않고 즉시 반환)
        if (payment.getStatus() == PaymentStatus.PAY_SUCCESS) {
            return PaymentConfirmResponse.of(payment);
        }

        // 3. Payment 상태 PAY_PENDING 검증
        if (payment.getStatus() != PaymentStatus.PAY_PENDING) {
            throw new CustomException(ErrorCode.INVALID_PAYMENT_STATUS);
        }

        // 4. 요청 금액(amount)과 결제 객체 금액 일치 검증 (위변조 2차 검증)
        if (!payment.getAmount().equals(request.getAmount())) {
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        try {
            // 5. 토스페이먼츠 승인 API 연동 호출
            TossConfirmResponseDto tossResponse = tossPaymentsClient.confirm(
                    request.getPaymentKey(),
                    request.getOrderId(),
                    request.getAmount()
            );

            // 6. 승인 성공 시: paymentKey 저장, PAY_SUCCESS 변경, 예약 CONFIRMED 변경, 타임슬롯 SLOT_FULL 변경
            LocalDateTime paidAt = LocalDateTime.parse(tossResponse.approvedAt(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            paymentConfirmHelper.saveConfirmSuccess(
                    payment.getId(),
                    request.getPaymentKey(),
                    tossResponse.method(),
                    paidAt
            );

            // 데이터 정합성이 확보된 최종 객체 재조회
            Payment confirmedPayment = paymentRepository.findById(payment.getId())
                    .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

            return PaymentConfirmResponse.of(confirmedPayment);

        } catch (Exception e) {
            // 7. 승인 실패 시: PAY_FAILED 처리 및 실패 사유 기록 (예약 및 슬롯 상태는 유지하여 스케줄러/TTL 처리 위임)
            paymentConfirmHelper.saveConfirmFailure(payment.getId(), e.getMessage());
            throw new CustomException(HttpStatus.BAD_REQUEST, "결제 승인 과정에서 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // 토스페이먼츠 웹훅 요청을 수신하여 서명을 검증하고 결제 상태를 업데이트합니다.
    @Transactional
    public void processWebhook(String payload, String signature, String transmissionTime) {
        // 웹훅 요청 서명 검증 수행
        verifyWebhookSignature(payload, signature, transmissionTime);

        PaymentWebhookRequest webhookRequest;
        try {
            webhookRequest = objectMapper.readValue(payload, PaymentWebhookRequest.class);
        } catch (Exception e) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "웹훅 바디 파싱에 실패했습니다.");
        }

        // eventType이 결제 상태 변경 이벤트(PAYMENT_STATUS_CHANGED)인지 확인
        if (!"PAYMENT_STATUS_CHANGED".equals(webhookRequest.getEventType())) {
            return;
        }

        PaymentWebhookRequest.WebhookData data = webhookRequest.getData();
        if (data == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "웹훅 상세 데이터가 존재하지 않습니다.");
        }

        // orderId 기준 결제 정보 조회
        Payment payment = paymentRepository.findByOrderId(data.getOrderId())
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        String status = data.getStatus();
        if ("DONE".equals(status)) {
            LocalDateTime approvedAt = null;
            if (data.getApprovedAt() != null) {
                approvedAt = LocalDateTime.parse(data.getApprovedAt(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            }
            paymentConfirmHelper.saveWebhookSuccess(
                    payment.getId(),
                    data.getPaymentKey(),
                    data.getMethod(),
                    approvedAt
            );
        } else if ("ABORTED".equals(status)) {
            paymentConfirmHelper.saveWebhookFailure(payment.getId(), "웹훅 수신: 결제 실패");
        } else if ("EXPIRED".equals(status)) {
            paymentConfirmHelper.saveWebhookTimeout(payment.getId());
        }
    }

    // 토스페이먼츠 웹훅 서명 검증을 진행합니다.
    private void verifyWebhookSignature(String payload, String signature, String transmissionTime) {
        if (signature == null || transmissionTime == null) {
            throw new CustomException(ErrorCode.WEBHOOK_VERIFICATION_FAILED);
        }

        try {
            // 검증 대상 메시지 생성
            String message = payload + ":" + transmissionTime;

            // HMAC SHA-256 해시 인스턴스 생성 및 키 초기화
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    webhookSecretKey.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            sha256HMAC.init(secretKeySpec);

            // 해시값 계산 및 Base64 인코딩
            byte[] hashBytes = sha256HMAC.doFinal(message.getBytes(StandardCharsets.UTF_8));
            String generatedSignature = Base64.getEncoder().encodeToString(hashBytes);

            // 콤마로 구분된 헤더 서명과 생성한 서명 비교
            String[] signatures = signature.split(",");
            boolean isMatched = false;
            for (String sig : signatures) {
                String actualSignature = sig.trim().startsWith("v1:") ? sig.trim().substring(3) : sig.trim();
                if (actualSignature.equals(generatedSignature)) {
                    isMatched = true;
                    break;
                }
            }

            if (!isMatched) {
                throw new CustomException(ErrorCode.WEBHOOK_VERIFICATION_FAILED);
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.WEBHOOK_VERIFICATION_FAILED);
        }
    }

    /**
     * 결제 환불(취소) 단계를 처리합니다.
     * 외부 PG API 호출로 인한 DB 커넥션 풀 고갈을 방지하고자 트랜잭션 없이 시작하여 내부 헬퍼 전이 메서드들을 호출합니다.
     *
     * @param paymentId 결제 ID
     * @param request 환불 요청 DTO
     * @return 결제 환불 결과 응답 DTO
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PaymentRefundResponse refundPayment(Long paymentId, PaymentRefundRequest request) {
        // 1. 사전 검증 트랜잭션 호출
        Payment payment = paymentRefundHelper.validateRefund(paymentId);

        // 2. 멱등성 보장 (이미 PAY_REFUNDED 상태라면 Toss API 호출 없이 성공 응답 반환)
        if (payment.getStatus() == PaymentStatus.PAY_REFUNDED) {
            return PaymentRefundResponse.of(payment);
        }

        String paymentKey = payment.getPaymentKey();
        String orderId = payment.getOrderId();

        try {
            // 3. 토스페이먼츠 결제 취소 API 연동 호출 (비트랜잭션)
            TossCancelResponseDto tossResponse = tossPaymentsClient.cancel(paymentKey, request.getCancelReason());

            LocalDateTime refundedAt = LocalDateTime.now();
            Integer refundAmount = payment.getAmount();

            if (tossResponse.cancels() != null && !tossResponse.cancels().isEmpty()) {
                TossCancelResponseDto.TossCancelDetail cancelDetail = tossResponse.cancels().get(0);
                if (cancelDetail.canceledAt() != null) {
                    refundedAt = LocalDateTime.parse(cancelDetail.canceledAt(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                }
                if (cancelDetail.cancelAmount() != null) {
                    refundAmount = cancelDetail.cancelAmount();
                }
            }

            // 4. 성공 시: PAY_REFUNDED 상태 저장 트랜잭션 호출
            paymentRefundHelper.saveRefundSuccess(
                    paymentId,
                    refundAmount,
                    refundedAt,
                    request.getCancelReason()
            );

            // 데이터 정합성이 확보된 최종 객체 재조회 후 응답
            Payment refundedPayment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

            return PaymentRefundResponse.of(refundedPayment);

        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            // Toss API 오류 - 4xx 또는 5xx
            log.error("Toss refund failed - reservationId: {}, paymentId: {}, orderId: {}, paymentKey: {}, cancelReason: {}, message: {}, body: {}",
                    payment.getReservation() != null ? payment.getReservation().getId() : null, paymentId, orderId, paymentKey, request.getCancelReason(), e.getMessage(), e.getResponseBodyAsString(), e);
            throw new CustomException(HttpStatus.valueOf(e.getStatusCode().value()), "토스 환불 API 호출 중 에러가 발생했습니다: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            // 네트워크 오류, 타임아웃, 예외 발생
            log.error("Toss refund failed (Network/Timeout/Unexpected) - reservationId: {}, paymentId: {}, orderId: {}, paymentKey: {}, cancelReason: {}, message: {}",
                    payment.getReservation() != null ? payment.getReservation().getId() : null, paymentId, orderId, paymentKey, request.getCancelReason(), e.getMessage(), e);
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "환불 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
