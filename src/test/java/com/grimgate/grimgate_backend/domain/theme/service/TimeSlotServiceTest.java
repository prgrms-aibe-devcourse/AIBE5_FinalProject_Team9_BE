package com.grimgate.grimgate_backend.domain.theme.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.grimgate.grimgate_backend.domain.theme.dto.AvailableSlotsResponse;
import com.grimgate.grimgate_backend.domain.theme.dto.TimeSlotInfo;
import com.grimgate.grimgate_backend.domain.theme.entity.Branch;
import com.grimgate.grimgate_backend.domain.theme.entity.Theme;
import com.grimgate.grimgate_backend.domain.theme.entity.TimeSlot;
import com.grimgate.grimgate_backend.domain.theme.entity.TimeSlotStatus;
import com.grimgate.grimgate_backend.domain.theme.repository.TimeSlotRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TimeSlotServiceTest {

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @InjectMocks
    private TimeSlotService timeSlotService;

    @Test
    @DisplayName("날짜가 주어지지 않으면 오늘부터 오늘+2일까지의 기본 날짜 범위가 적용되어야 한다")
    void shouldApplyDefaultDateRangeWhenDatesAreNull() {
        // given
        LocalDate today = LocalDate.now();
        LocalDate todayPlus2 = today.plusDays(2);
        
        when(timeSlotRepository.findAvailableSlotsWithFilters(
                eq(TimeSlotStatus.SLOT_AVAILABLE),
                eq(today),
                eq(todayPlus2),
                any(), any(), any(), any(), any()
        )).thenReturn(Collections.emptyList());

        // when
        timeSlotService.getAvailableSlots(
                null, null, null, null, null, null, null, "rating_desc"
        );

        // then
        verify(timeSlotRepository).findAvailableSlotsWithFilters(
                eq(TimeSlotStatus.SLOT_AVAILABLE),
                eq(today),
                eq(todayPlus2),
                any(), any(), any(), any(), any()
        );
    }

    @Test
    @DisplayName("기본 정렬(rating_desc)일 때 평점 높은 순으로 테마가 정렬되고 각 테마 내부 슬롯은 시작시간 오름차순이어야 한다")
    void shouldSortByRatingDescDefault() {
        // given
        Branch branch = Branch.builder()
                .id(1L)
                .branchName("강남점")
                .region("서울")
                .build();

        Theme themeLowRating = Theme.builder()
                .id(10L)
                .title("낮은평점테마")
                .branch(branch)
                .rating(BigDecimal.valueOf(3.5))
                .build();

        Theme themeHighRating = Theme.builder()
                .id(20L)
                .title("높은평점테마")
                .branch(branch)
                .rating(BigDecimal.valueOf(4.8))
                .build();

        // 10번 테마 슬롯 2개 (순서 섞어서 모의 데이터 구성)
        TimeSlot slotLow2 = TimeSlot.builder()
                .id(102L)
                .theme(themeLowRating)
                .slotDate(LocalDate.now())
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .status(TimeSlotStatus.SLOT_AVAILABLE)
                .build();

        TimeSlot slotLow1 = TimeSlot.builder()
                .id(101L)
                .theme(themeLowRating)
                .slotDate(LocalDate.now())
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(15, 0))
                .status(TimeSlotStatus.SLOT_AVAILABLE)
                .build();

        // 20번 테마 슬롯 1개
        TimeSlot slotHigh1 = TimeSlot.builder()
                .id(201L)
                .theme(themeHighRating)
                .slotDate(LocalDate.now())
                .startTime(LocalTime.of(16, 0))
                .endTime(LocalTime.of(17, 0))
                .status(TimeSlotStatus.SLOT_AVAILABLE)
                .build();

        when(timeSlotRepository.findAvailableSlotsWithFilters(
                any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(Arrays.asList(slotLow2, slotLow1, slotHigh1));

        // when
        List<AvailableSlotsResponse> result = timeSlotService.getAvailableSlots(
                null, null, null, null, null, null, null, "rating_desc"
        );

        // then
        assertThat(result).hasSize(2);
        // 평점이 높은 테마(4.8)가 먼저 정렬되어야 한다
        assertThat(result.get(0).getThemeId()).isEqualTo(20L);
        assertThat(result.get(1).getThemeId()).isEqualTo(10L);

        // 10번 테마의 슬롯들은 14:00가 18:00보다 먼저 와야 한다 (시간 오름차순 정렬 검증)
        List<TimeSlotInfo> lowThemeSlots = result.get(1).getAvailableSlots();
        assertThat(lowThemeSlots).hasSize(2);
        assertThat(lowThemeSlots.get(0).getTimeSlotId()).isEqualTo(101L);
        assertThat(lowThemeSlots.get(1).getTimeSlotId()).isEqualTo(102L);
    }

    @Test
    @DisplayName("start_time_asc 정렬 요청 시 가장 빠른 예약 슬롯을 가진 테마가 먼저 정렬되어야 한다")
    void shouldSortByStartTimeAsc() {
        // given
        Branch branch = Branch.builder()
                .id(1L)
                .branchName("강남점")
                .region("서울")
                .build();

        Theme themeLate = Theme.builder()
                .id(10L)
                .title("늦은예약테마")
                .branch(branch)
                .rating(BigDecimal.valueOf(4.5))
                .build();

        Theme themeEarly = Theme.builder()
                .id(20L)
                .title("빠른예약테마")
                .branch(branch)
                .rating(BigDecimal.valueOf(4.0))
                .build();

        // 10번 테마: 오늘 18:00 슬롯
        TimeSlot slotLate = TimeSlot.builder()
                .id(101L)
                .theme(themeLate)
                .slotDate(LocalDate.now())
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .status(TimeSlotStatus.SLOT_AVAILABLE)
                .build();

        // 20번 테마: 오늘 10:00 슬롯 (더 빠름)
        TimeSlot slotEarly = TimeSlot.builder()
                .id(201L)
                .theme(themeEarly)
                .slotDate(LocalDate.now())
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .status(TimeSlotStatus.SLOT_AVAILABLE)
                .build();

        when(timeSlotRepository.findAvailableSlotsWithFilters(
                any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(Arrays.asList(slotLate, slotEarly));

        // when
        List<AvailableSlotsResponse> result = timeSlotService.getAvailableSlots(
                null, null, null, null, null, null, null, "start_time_asc"
        );

        // then
        assertThat(result).hasSize(2);
        // 가장 빠른 슬롯이 있는 테마(20L, 10:00)가 먼저 와야 한다
        assertThat(result.get(0).getThemeId()).isEqualTo(20L);
        assertThat(result.get(1).getThemeId()).isEqualTo(10L);
    }
}
