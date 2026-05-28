package com.grimgate.grimgate_backend.domain.theme.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 빠른예약 조회 API의 테마별 예약 가능 슬롯 목록을 담는 응답 DTO 클래스입니다.
 * 
 * [이 DTO가 필요한 이유]
 * - 프론트엔드 화면(빠른예약)에서 각 테마의 상세 정보와 해당 테마의 예약 가능 슬롯 목록을
 *   한눈에 구조화하여 렌더링할 수 있도록 돕는 프레젠테이션 객체입니다.
 */
@Getter
public class AvailableSlotsResponse {

    private final Long themeId;
    private final String themeTitle;
    private final Long branchId;
    private final String branchName;
    private final String region;
    private final BigDecimal rating;
    private final Integer reviewCount;
    private final Integer difficulty;
    private final Integer horrorLevel;
    private final Integer minPeople;
    private final Integer maxPeople;
    private final Integer price;
    private final String thumbnailUrl;
    private final List<TimeSlotInfo> availableSlots;

    @Builder
    public AvailableSlotsResponse(Long themeId, String themeTitle, Long branchId, String branchName,
                                  String region, BigDecimal rating, Integer reviewCount, Integer difficulty,
                                  Integer horrorLevel, Integer minPeople, Integer maxPeople, Integer price,
                                  String thumbnailUrl, List<TimeSlotInfo> availableSlots) {
        this.themeId = themeId;
        this.themeTitle = themeTitle;
        this.branchId = branchId;
        this.branchName = branchName;
        this.region = region;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.difficulty = difficulty;
        this.horrorLevel = horrorLevel;
        this.minPeople = minPeople;
        this.maxPeople = maxPeople;
        this.price = price;
        this.thumbnailUrl = thumbnailUrl;
        this.availableSlots = availableSlots;
    }
}
