package com.grimgate.grimgate_backend.domain.mate.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 메이트 모집 통계 응답 DTO. — API 명세 3-7 기준
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MatePostStatsResponse {

    /** 오늘 등록된 신규 모집글 수 */
    private long todayNewCount;

    /** 모집 중인 글 수 (RECRUITING + CLOSING_SOON) */
    private long recruitingCount;

    /** 누적 매칭 완료 글 수 */
    private long totalMatchedCount;
}
