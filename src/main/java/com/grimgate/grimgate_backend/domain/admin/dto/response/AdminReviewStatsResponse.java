package com.grimgate.grimgate_backend.domain.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

// 관리자 후기 통계 카드 응답 DTO
@Getter
@Builder
public class AdminReviewStatsResponse {

    private long totalReviews;    // 전체 후기 수
    private long activeReviews;   // 정상(ACTIVE) 후기 수
    private long hiddenReviews;   // 숨김(HIDDEN) 후기 수
    private long pendingReports;  // 관리자 검토 대기 신고 수 (REQUESTED_ADMIN_REVIEW)

    public static AdminReviewStatsResponse of(long total, long active, long hidden, long pending) {
        return AdminReviewStatsResponse.builder()
                .totalReviews(total)
                .activeReviews(active)
                .hiddenReviews(hidden)
                .pendingReports(pending)
                .build();
    }
}
