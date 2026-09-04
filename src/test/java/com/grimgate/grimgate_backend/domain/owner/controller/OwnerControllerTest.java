package com.grimgate.grimgate_backend.domain.owner.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.grimgate.grimgate_backend.domain.owner.dto.OwnerReservationResponse;
import com.grimgate.grimgate_backend.domain.owner.dto.OwnerReservationSearchRequest;
import com.grimgate.grimgate_backend.domain.owner.dto.OwnerReservationStatsResponse;

import com.grimgate.grimgate_backend.domain.owner.service.OwnerService;
import com.grimgate.grimgate_backend.domain.reservation.entity.ReservationStatus;
import com.grimgate.grimgate_backend.domain.review.service.ReviewReportService;
import com.grimgate.grimgate_backend.global.S3.S3Uploader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@WebMvcTest(OwnerController.class)
@AutoConfigureMockMvc(addFilters = false)
class OwnerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OwnerService ownerService;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockBean
    private S3Uploader s3Uploader;

    @MockBean
    private ReviewReportService reviewReportService;

    @Test
    @DisplayName("GET /api/owner/reservations - 예약 검색 API 성공 시 200 OK와 페이징 정보 반환")
    void searchReservations_Success() throws Exception {
        // given
        OwnerReservationResponse responseDto = OwnerReservationResponse.builder()
                .reservationId(10L)
                .reservationDate(LocalDate.of(2026, 6, 10))
                .reservationTime(LocalTime.of(15, 30))
                .themeTitle("탈출하라")
                .nickname("도전자")
                .phone("010-0000-0000")
                .peopleCount(4)
                .status(ReservationStatus.CONFIRMED)
                .escapeResult("성공 (42:10)")
                .build();

        Page<OwnerReservationResponse> page = new PageImpl<>(List.of(responseDto), PageRequest.of(0, 10), 1);

        when(ownerService.searchReservations(any(OwnerReservationSearchRequest.class), any(Pageable.class)))
                .thenReturn(page);

        // when & then
        mockMvc.perform(get("/api/owner/reservations")
                        .param("startDate", "2026-06-01")
                        .param("endDate", "2026-06-30")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("예약 목록 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data.content[0].reservationId").value(10))
                .andExpect(jsonPath("$.data.content[0].themeTitle").value("탈출하라"))
                .andExpect(jsonPath("$.data.content[0].nickname").value("도전자"))
                .andExpect(jsonPath("$.data.content[0].escapeResult").value("성공 (42:10)"));
     }

    @Test
    @DisplayName("GET /api/owner/reservations/stats - 예약 통계 조회 API 성공 시 200 OK와 통계 정보 반환")
    void getReservationStats_Success() throws Exception {
        // given
        OwnerReservationStatsResponse responseDto = OwnerReservationStatsResponse.builder()
                .totalCount(10L)
                .todayCount(2L)
                .completedCount(5L)
                .confirmedCount(3L)
                .cancelledCount(2L)
                .build();

        // date_from, date_to 파라미터가 null일 수 있는 것을 대비하여, mock에 flexible argument matching 지정
        when(ownerService.getReservationStats(any(), any()))
                .thenReturn(responseDto);

        // when & then
        mockMvc.perform(get("/api/owner/reservations/stats")
                        .param("dateFrom", "2026-06-01")
                        .param("dateTo", "2026-06-30")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("예약 통계 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data.total_count").value(10))
                .andExpect(jsonPath("$.data.today_count").value(2))
                .andExpect(jsonPath("$.data.completed_count").value(5))
                .andExpect(jsonPath("$.data.confirmed_count").value(3))
                .andExpect(jsonPath("$.data.cancelled_count").value(2));
    }
}

