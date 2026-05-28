package com.grimgate.grimgate_backend.domain.theme.controller;

import com.grimgate.grimgate_backend.domain.theme.dto.AvailableSlotsResponse;
import com.grimgate.grimgate_backend.domain.theme.service.TimeSlotService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 빠른예약 예약 가능 시간 조회 API를 제공하는 Controller 클래스입니다.
 * API 경로: GET /api/slots/available
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/slots")
public class TimeSlotController {

    private final TimeSlotService timeSlotService;

    /**
     * 빠른예약 페이지에서 필터 조건에 부합하는 여러 테마의 예약 가능한 타임슬롯 목록을 테마별로 묶어 조회합니다.
     *
     * @param region      지역 (선택)
     * @param dateFrom    시작 날짜 (선택, yyyy-MM-dd)
     * @param dateTo      종료 날짜 (선택, yyyy-MM-dd)
     * @param peopleCount 이용할 인원 수 (선택)
     * @param horrorLevel 공포도 (선택)
     * @param difficulty  난이도 (선택)
     * @param minRating   최저 평점 (선택)
     * @param sort        정렬 기준 (선택, 기본값 rating_desc)
     * @return 예약 가능한 테마 및 슬롯 목록 DTO 리스트
     * 
     * [빠른예약 조회에서 이 컨트롤러가 필요한 이유]
     * - 클라이언트가 단 한 번의 GET API 호출을 통해 지역, 인원, 난이도 등 다양한 동적 조건을 필터링하여
     *   예약이 즉시 가능한 모든 테마와 슬롯 목록을 수집할 수 있도록 통합 진입점을 제공합니다.
     * - 각 파라미터를 Optional하게 처리함으로써 사용자가 특정 필터만 선택(예: 지역만 지정하거나 날짜만 지정)하여
     *   빠른 검색을 유연하게 할 수 있도록 돕습니다.
     */
    @GetMapping("/available")
    public ResponseEntity<List<AvailableSlotsResponse>> getAvailableSlots(
            @RequestParam(value = "region", required = false) String region,
            @RequestParam(value = "date_from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(value = "date_to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(value = "people_count", required = false) Integer peopleCount,
            @RequestParam(value = "horror_level", required = false) Integer horrorLevel,
            @RequestParam(value = "difficulty", required = false) Integer difficulty,
            @RequestParam(value = "min_rating", required = false) BigDecimal minRating,
            @RequestParam(value = "sort", required = false, defaultValue = "rating_desc") String sort
    ) {
        List<AvailableSlotsResponse> response = timeSlotService.getAvailableSlots(
                region, dateFrom, dateTo, peopleCount, horrorLevel, difficulty, minRating, sort
        );
        return ResponseEntity.ok(response);
    }
}
