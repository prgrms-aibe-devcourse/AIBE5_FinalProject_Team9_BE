package com.grimgate.grimgate_backend.domain.reservation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grimgate.grimgate_backend.domain.payment.entity.Payment;
import com.grimgate.grimgate_backend.domain.payment.service.PaymentService;
import com.grimgate.grimgate_backend.domain.reservation.dto.ReservationCancelResponse;
import com.grimgate.grimgate_backend.domain.reservation.dto.ReservationCreateRequest;
import com.grimgate.grimgate_backend.domain.reservation.dto.ReservationCreateResponse;
import com.grimgate.grimgate_backend.domain.reservation.service.ReservationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(ReservationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReservationService reservationService;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("POST /api/reservations - 예약 생성 성공 시 200 OK와 정보 반환")
    void createReservation_Success() throws Exception {
        // given
        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .timeSlotId(10L)
                .holdToken("hold-token-123")
                .peopleCount(3)
                .termsAgreed(true)
                .build();

        ReservationCreateResponse response = ReservationCreateResponse.builder()
                .reservationId(50L)
                .timeSlotId(10L)
                .memberId(1L)
                .status("PENDING_PAYMENT")
                .peopleCount(3)
                .totalPrice(66000)
                .build();

        when(reservationService.createReservation(any(ReservationCreateRequest.class)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value(50))
                .andExpect(jsonPath("$.timeSlotId").value(10))
                .andExpect(jsonPath("$.memberId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.peopleCount").value(3))
                .andExpect(jsonPath("$.totalPrice").value(66000));
    }

    @Test
    @DisplayName("POST /api/reservations - 필수 파라미터 누락 시 400 Bad Request 반환")
    void createReservation_InvalidInput() throws Exception {
        // given
        ReservationCreateRequest request = ReservationCreateRequest.builder()
                // timeSlotId 누락
                .holdToken("")
                .peopleCount(0) // 1 미만
                .build();

        // when & then
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/reservations - 선점 정보가 존재하지 않을 때 404 Not Found 반환")
    void createReservation_NotFoundException() throws Exception {
        // given
        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .timeSlotId(10L)
                .holdToken("hold-token-123")
                .peopleCount(3)
                .termsAgreed(true)
                .build();

        when(reservationService.createReservation(any(ReservationCreateRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "선점 정보가 존재하지 않습니다."));

        // when & then
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/reservations - 이미 선점된 슬롯이거나 선점 정보 불일치 시 409 Conflict 반환")
    void createReservation_ConflictException() throws Exception {
        // given
        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .timeSlotId(10L)
                .holdToken("hold-token-123")
                .peopleCount(3)
                .termsAgreed(true)
                .build();

        when(reservationService.createReservation(any(ReservationCreateRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "선점 정보가 일치하지 않습니다."));

        // when & then
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/reservations/{reservationId}/cancel - 예약 취소 성공 시 200 OK와 ApiResponse 형식의 결과 반환 (환불 미대상)")
    void cancelReservation_Success_NoRefund() throws Exception {
        // given
        Long reservationId = 50L;
        ReservationCancelResponse response = ReservationCancelResponse.builder()
                .reservationId(reservationId)
                .status("CANCELLED")
                .paymentId(null)
                .build();

        when(reservationService.cancelReservation(reservationId)).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/reservations/{reservationId}/cancel", reservationId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("예약이 취소되었습니다."))
                .andExpect(jsonPath("$.data.reservationId").value(50))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.paymentId").value(org.hamcrest.CoreMatchers.nullValue()));
    }

    @Test
    @DisplayName("POST /api/reservations/{reservationId}/cancel - CONFIRMED 예약 취소 성공 및 자동 환불 정상 연계")
    void cancelReservation_Success_WithRefund() throws Exception {
        // given
        Long reservationId = 50L;
        Long paymentId = 100L;
        ReservationCancelResponse response = ReservationCancelResponse.builder()
                .reservationId(reservationId)
                .status("CANCELLED")
                .paymentId(paymentId)
                .build();

        when(reservationService.cancelReservation(reservationId)).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/reservations/{reservationId}/cancel", reservationId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("예약이 취소되었습니다."))
                .andExpect(jsonPath("$.data.reservationId").value(50))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.paymentId").value(100));

        org.mockito.Mockito.verify(paymentService).refundPayment(
                eq(paymentId),
                any(com.grimgate.grimgate_backend.domain.payment.dto.PaymentRefundRequest.class)
        );
    }

    @Test
    @DisplayName("POST /api/reservations/{reservationId}/cancel - Toss 환불 실패 시에도 예약 취소는 성공으로 반환")
    void cancelReservation_RefundFailed_StillSucceeds() throws Exception {
        // given
        Long reservationId = 50L;
        Long paymentId = 100L;
        ReservationCancelResponse response = ReservationCancelResponse.builder()
                .reservationId(reservationId)
                .status("CANCELLED")
                .paymentId(paymentId)
                .build();

        when(reservationService.cancelReservation(reservationId)).thenReturn(response);
        org.mockito.Mockito.doThrow(new RuntimeException("Toss API Error"))
                .when(paymentService).refundPayment(eq(paymentId), any(com.grimgate.grimgate_backend.domain.payment.dto.PaymentRefundRequest.class));

        // when & then
        mockMvc.perform(post("/api/reservations/{reservationId}/cancel", reservationId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("예약이 취소되었습니다."))
                .andExpect(jsonPath("$.data.reservationId").value(50))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        org.mockito.Mockito.verify(paymentService).refundPayment(
                eq(paymentId),
                any(com.grimgate.grimgate_backend.domain.payment.dto.PaymentRefundRequest.class)
        );
    }
}
