package com.grimgate.grimgate_backend.domain.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grimgate.grimgate_backend.domain.payment.dto.PaymentReadyRequest;
import com.grimgate.grimgate_backend.domain.payment.dto.PaymentReadyResponse;
import com.grimgate.grimgate_backend.domain.payment.dto.PaymentConfirmRequest;
import com.grimgate.grimgate_backend.domain.payment.dto.PaymentConfirmResponse;
import com.grimgate.grimgate_backend.domain.payment.dto.PaymentRefundRequest;
import com.grimgate.grimgate_backend.domain.payment.dto.PaymentRefundResponse;
import com.grimgate.grimgate_backend.domain.payment.entity.PaymentStatus;
import com.grimgate.grimgate_backend.domain.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("POST /api/payments/ready - 결제 준비 요청 성공")
    void readyPayment_Success() throws Exception {
        // given
        PaymentReadyRequest request = PaymentReadyRequest.builder()
                .reservationId(100L)
                .amount(22000)
                .build();

        PaymentReadyResponse response = PaymentReadyResponse.builder()
                .paymentId(1L)
                .reservationId(100L)
                .orderId("PAY-ORDER-123")
                .amount(22000)
                .status(PaymentStatus.PAY_PENDING)
                .orderName("공포의 방")
                .customerName("테스터")
                .customerEmail("test@test.com")
                .build();

        when(paymentService.readyPayment(any(PaymentReadyRequest.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/payments/ready")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("결제 준비가 완료되었습니다."))
                .andExpect(jsonPath("$.data.paymentId").value(1))
                .andExpect(jsonPath("$.data.reservationId").value(100))
                .andExpect(jsonPath("$.data.orderId").value("PAY-ORDER-123"))
                .andExpect(jsonPath("$.data.amount").value(22000))
                .andExpect(jsonPath("$.data.status").value("PAY_PENDING"))
                .andExpect(jsonPath("$.data.orderName").value("공포의 방"))
                .andExpect(jsonPath("$.data.customerName").value("테스터"))
                .andExpect(jsonPath("$.data.customerEmail").value("test@test.com"));
    }

    @Test
    @DisplayName("POST /api/payments/ready - 예약 ID 누락 시 400 BAD_REQUEST 반환")
    void readyPayment_ValidationFailure_MissingReservationId() throws Exception {
        // given
        PaymentReadyRequest request = PaymentReadyRequest.builder()
                .amount(22000)
                .build();

        // when & then
        mockMvc.perform(post("/api/payments/ready")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("예약 ID는 필수입니다."));
    }

    @Test
    @DisplayName("POST /api/payments/ready - 결제 금액 0 이하일 때 400 BAD_REQUEST 반환")
    void readyPayment_ValidationFailure_InvalidAmount() throws Exception {
        // given
        PaymentReadyRequest request = PaymentReadyRequest.builder()
                .reservationId(100L)
                .amount(-1000)
                .build();

        // when & then
        mockMvc.perform(post("/api/payments/ready")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("결제 금액은 0보다 커야 합니다."));
    }

    @Test
    @DisplayName("POST /api/payments/confirm - 결제 승인 요청 성공")
    void confirmPayment_Success() throws Exception {
        // given
        PaymentConfirmRequest request = PaymentConfirmRequest.builder()
                .paymentKey("toss-key-123")
                .orderId("order-uuid-xyz")
                .amount(22000)
                .build();

        PaymentConfirmResponse response = PaymentConfirmResponse.builder()
                .paymentId(1L)
                .reservationId(100L)
                .orderId("order-uuid-xyz")
                .amount(22000)
                .status(PaymentStatus.PAY_SUCCESS)
                .paymentKey("toss-key-123")
                .paymentMethod("카드")
                .paidAt(java.time.LocalDateTime.now())
                .build();

        when(paymentService.confirmPayment(any(PaymentConfirmRequest.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("결제 승인이 완료되었습니다."))
                .andExpect(jsonPath("$.data.paymentId").value(1))
                .andExpect(jsonPath("$.data.orderId").value("order-uuid-xyz"))
                .andExpect(jsonPath("$.data.status").value("PAY_SUCCESS"))
                .andExpect(jsonPath("$.data.paymentKey").value("toss-key-123"))
                .andExpect(jsonPath("$.data.paymentMethod").value("카드"));
    }

    @Test
    @DisplayName("POST /api/payments/confirm - 필수 요청 값 누락 시 400 BAD_REQUEST 반환")
    void confirmPayment_ValidationFailure_MissingFields() throws Exception {
        // given
        PaymentConfirmRequest request = PaymentConfirmRequest.builder()
                .paymentKey("") // 빈값
                .orderId("order-uuid-xyz")
                .amount(22000)
                .build();

        // when & then
        mockMvc.perform(post("/api/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("결제 고유 키(paymentKey)는 필수입니다."));
    }

    @Test
    @DisplayName("POST /api/payments/webhook - 웹훅 요청 성공")
    void handleWebhook_Success() throws Exception {
        // given
        String payload = "{\"eventType\":\"PAYMENT_STATUS_CHANGED\"}";
        String signature = "v1:signature-string";
        String transmissionTime = "2026-06-10T12:00:10+09:00";

        // when & then
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("tosspayments-webhook-signature", signature)
                        .header("tosspayments-webhook-transmission-time", transmissionTime)
                        .content(payload))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(paymentService).processWebhook(payload, signature, transmissionTime);
    }

    @Test
    @DisplayName("POST /api/payments/{paymentId}/refund - 결제 환불 요청 성공")
    void refundPayment_Success() throws Exception {
        // given
        Long paymentId = 1L;
        PaymentRefundRequest request = PaymentRefundRequest.builder()
                .cancelReason("고객 변심")
                .build();

        PaymentRefundResponse response = PaymentRefundResponse.builder()
                .paymentId(paymentId)
                .orderId("order-uuid-xyz")
                .status(PaymentStatus.PAY_REFUNDED)
                .refundAmount(22000)
                .refundedAt(java.time.LocalDateTime.now())
                .cancelReason("고객 변심")
                .build();

        when(paymentService.refundPayment(any(Long.class), any(PaymentRefundRequest.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/payments/{paymentId}/refund", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("환불 처리가 완료되었습니다."))
                .andExpect(jsonPath("$.data.paymentId").value(paymentId))
                .andExpect(jsonPath("$.data.status").value("PAY_REFUNDED"))
                .andExpect(jsonPath("$.data.cancelReason").value("고객 변심"));
    }

    @Test
    @DisplayName("POST /api/payments/{paymentId}/refund - 환불 사유 누락 시 400 BAD_REQUEST 반환")
    void refundPayment_ValidationFailure_MissingReason() throws Exception {
        // given
        Long paymentId = 1L;
        PaymentRefundRequest request = PaymentRefundRequest.builder()
                .cancelReason("") // 빈 사유
                .build();

        // when & then
        mockMvc.perform(post("/api/payments/{paymentId}/refund", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("환불 사유는 필수입니다."));
    }
}
