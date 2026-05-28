package com.grimgate.grimgate_backend.domain.theme.service;

import com.grimgate.grimgate_backend.domain.theme.dto.AvailableSlotsResponse;
import com.grimgate.grimgate_backend.domain.theme.dto.TimeSlotInfo;
import com.grimgate.grimgate_backend.domain.theme.entity.Theme;
import com.grimgate.grimgate_backend.domain.theme.entity.TimeSlot;
import com.grimgate.grimgate_backend.domain.theme.entity.TimeSlotStatus;
import com.grimgate.grimgate_backend.domain.theme.repository.TimeSlotRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 빠른예약 타임슬롯 조회 및 필터링 처리를 담당하는 Service 클래스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;

    /**
     * 필터 조건에 부합하는 예약 가능한 타임슬롯을 조회하고 테마별로 묶어 응답합니다.
     *
     * @param region       지역 검색어 (선택)
     * @param dateFrom     조회 시작 일자 (선택)
     * @param dateTo       조회 종료 일자 (선택)
     * @param peopleCount  이용 인원수 (선택)
     * @param horrorLevel  공포도 (선택)
     * @param difficulty   난이도 (선택)
     * @param minRating    최저 평점 (선택)
     * @param sort         정렬 기준 (선택, 기본값 rating_desc)
     * @return 테마별 예약 가능 슬롯 정보 목록
     * 
     * [빠른예약 조회에서 이 로직이 필요한 이유]
     * - 날짜 범위의 유효한 예약창을 확보하기 위해 date_from, date_to 파라미터가 비어있을 시 
     *   "오늘부터 3일(오늘, 오늘+1, 오늘+2)"의 기본 범위를 동적으로 설정합니다.
     * - `SLOT_AVAILABLE` 상태의 타임슬롯 데이터만 DB로부터 필터링하여 불필요한 결제 진행중/완료 건 조회를 사전에 차단합니다.
     * - 정렬 옵션에 따라 "평점 높은 순(rating_desc)" 또는 "가장 빠른 예약 가능 시간이 있는 테마 순(start_time_asc)"으로
     *   유연하게 응답을 변경하여 사용자 맞춤 추천 정렬 기능을 보장합니다.
     */
    public List<AvailableSlotsResponse> getAvailableSlots(
            String region,
            LocalDate dateFrom,
            LocalDate dateTo,
            Integer peopleCount,
            Integer horrorLevel,
            Integer difficulty,
            Double minRating,
            String sort
    ) {
        // 1. 날짜 기본값 설정 (오늘 ~ 오늘+2일)
        LocalDate actualDateFrom = (dateFrom != null) ? dateFrom : LocalDate.now();
        LocalDate actualDateTo = (dateTo != null) ? dateTo : actualDateFrom.plusDays(2);

        // 2. 지역명 공백 및 null 정규화 처리
        String actualRegion = (region != null && !region.trim().isEmpty()) ? region : null;

        // 3. 필터 조건에 부합하는 Available 상태의 타임슬롯 조회
        List<TimeSlot> slots = timeSlotRepository.findAvailableSlotsWithFilters(
                TimeSlotStatus.SLOT_AVAILABLE,
                actualDateFrom,
                actualDateTo,
                actualRegion,
                peopleCount,
                horrorLevel,
                difficulty,
                minRating
        );

        // 4. 조회된 슬롯들을 테마별로 그룹화
        Map<Theme, List<TimeSlot>> slotsByTheme = slots.stream()
                .collect(Collectors.groupingBy(TimeSlot::getTheme));

        // 5. 각 그룹 데이터를 DTO 형식으로 변환 및 각 테마 내 슬롯들 시간순 정렬
        List<AvailableSlotsResponse> responses = slotsByTheme.entrySet().stream()
                .map(entry -> {
                    Theme theme = entry.getKey();
                    
                    // 각 테마의 슬롯들은 날짜/시간 오름차순(chronological)으로 정렬
                    List<TimeSlotInfo> slotInfos = entry.getValue().stream()
                            .sorted(Comparator.comparing(TimeSlot::getSlotDate)
                                              .thenComparing(TimeSlot::getStartTime))
                            .map(ts -> TimeSlotInfo.builder()
                                    .timeSlotId(ts.getId())
                                    .slotDate(ts.getSlotDate())
                                    .startTime(ts.getStartTime())
                                    .endTime(ts.getEndTime())
                                    .build())
                            .toList();

                    return AvailableSlotsResponse.builder()
                            .themeId(theme.getId())
                            .themeTitle(theme.getTitle())
                            .branchId(theme.getBranch().getId())
                            .branchName(theme.getBranch().getBranchName())
                            .region(theme.getBranch().getRegion())
                            .rating(theme.getRating())
                            .reviewCount(theme.getReviewCount())
                            .difficulty(theme.getDifficulty())
                            .horrorLevel(theme.getHorrorLevel())
                            .minPeople(theme.getMinPeople())
                            .maxPeople(theme.getMaxPeople())
                            .price(theme.getPrice())
                            .thumbnailUrl(theme.getThumbnailUrl())
                            .availableSlots(slotInfos)
                            .build();
                })
                .collect(Collectors.toCollection(java.util.ArrayList::new));

        // 6. 요청 정렬 옵션에 따라 테마들을 최종 정렬
        if ("start_time_asc".equalsIgnoreCase(sort)) {
            // 가장 빠른 예약 가능 슬롯을 가진 테마가 앞으로 가도록 정렬
            responses.sort((r1, r2) -> {
                if (r1.getAvailableSlots().isEmpty() && r2.getAvailableSlots().isEmpty()) {
                    return 0;
                }
                if (r1.getAvailableSlots().isEmpty()) {
                    return 1;
                }
                if (r2.getAvailableSlots().isEmpty()) {
                    return -1;
                }

                TimeSlotInfo s1 = r1.getAvailableSlots().get(0);
                TimeSlotInfo s2 = r2.getAvailableSlots().get(0);

                LocalDateTime ldt1 = LocalDateTime.of(s1.getSlotDate(), s1.getStartTime());
                LocalDateTime ldt2 = LocalDateTime.of(s2.getSlotDate(), s2.getStartTime());

                int timeComp = ldt1.compareTo(ldt2);
                if (timeComp != 0) {
                    return timeComp;
                }
                
                // 1순위 시간 조건이 같은 경우, 평점이 더 높은 테마가 우선 노출
                Double rating1 = r1.getRating() != null ? r1.getRating() : 0.0;
                Double rating2 = r2.getRating() != null ? r2.getRating() : 0.0;
                int ratingComp = rating2.compareTo(rating1);
                if (ratingComp != 0) {
                    return ratingComp;
                }
                
                return r1.getThemeId().compareTo(r2.getThemeId());
            });
        } else {
            // 기본 정렬: 평점 높은 순(rating_desc)
            responses.sort((r1, r2) -> {
                Double rating1 = r1.getRating() != null ? r1.getRating() : 0.0;
                Double rating2 = r2.getRating() != null ? r2.getRating() : 0.0;
                int ratingComp = rating2.compareTo(rating1);
                if (ratingComp != 0) {
                    return ratingComp;
                }
                // 평점이 같은 경우 테마 ID 순
                return r1.getThemeId().compareTo(r2.getThemeId());
            });
        }

        return responses;
    }
}
