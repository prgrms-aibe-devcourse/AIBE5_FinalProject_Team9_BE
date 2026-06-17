package com.grimgate.grimgate_backend.domain.owner.dto;

import com.grimgate.grimgate_backend.domain.review.entity.ReviewReport;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 사장님 후기 신고 목록 조회 응답 DTO
@Getter
@Builder
public class OwnerReviewReportResponse {

    // 신고 ID
    private Long reportId;

    // 신고 대상 후기 ID
    private Long reviewId;

    // 후기 본문
    private String reviewContent;

    // 신고자 닉네임
    private String reporterNickname;

    // 신고 사유
    private String reason;

    // 신고 상세 내용
    private String detail;

    // 신고 처리 상태
    private String status;

    // 신고 접수 일시
    private LocalDateTime createdAt;
    private Integer rating;
    private Boolean spoiler;
    private String themeTitle;

    //사장님 신고 사유
    private String ownerReason;

    public static OwnerReviewReportResponse from(ReviewReport report) {
        return OwnerReviewReportResponse.builder()
                .reportId(report.getId())
                .reviewId(report.getReview().getId())
                .reviewContent(report.getReview().getContent())
                .reporterNickname(report.getReporter().getAccount().getNickname())
                .reason(report.getReason())
                .detail(report.getDetail())
                .status(report.getStatus().name())
                .createdAt(report.getCreatedAt())
                .rating(report.getReview().getRating())      // 추가
                .spoiler(report.getReview().getSpoiler())
                .themeTitle(report.getReview().getTheme().getTitle())
                .ownerReason(report.getOwnerReason())
                .build();
    }
}
