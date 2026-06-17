package com.grimgate.grimgate_backend.domain.admin.dto.response;

import com.grimgate.grimgate_backend.domain.review.entity.ReviewReport;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 관리자 신고 목록 조회 응답 DTO
 */
@Getter
@Builder
public class AdminReviewReportResponse {

    /** 신고 ID */
    private Long id;

    /** 신고 대상 후기 ID */
    private Long reviewId;

    /** 신고 대상 후기 본문 */
    private String reviewContent;

    /** 신고자 회원 ID */
    private Long reporterId;

    /** 신고자 닉네임 */
    private String reporterNickname;

    /** 오너 처리 사유 */
    private String ownerReason;

    /** 신고 처리 상태 */
    private String status;

    /** 신고 생성일시 */
    private LocalDateTime createdAt;

    private String themeTitle;
    private Integer rating;
    private String adminReason;

    /**
     * ReviewReport 엔티티로부터 응답 DTO 생성
     */
    public static AdminReviewReportResponse from(ReviewReport report) {
        return AdminReviewReportResponse.builder()
                .id(report.getId())
                .reviewId(report.getReview().getId())
                .reviewContent(report.getReview().getContent())
                .reporterId(report.getReporter().getId())
                .reporterNickname(report.getReporter().getAccount().getNickname())
                .ownerReason(report.getOwnerReason())
                .status(report.getStatus().name())
                .createdAt(report.getCreatedAt())
                .themeTitle(report.getReview().getTheme().getTitle())
                .rating(report.getReview().getRating())
                .adminReason(report.getAdminReason())
                .build();
    }
}
