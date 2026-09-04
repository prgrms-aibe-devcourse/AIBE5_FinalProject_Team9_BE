package com.grimgate.grimgate_backend.domain.review.entity;

/**
 * 후기 신고 처리 상태를 나타내는 열거형
 */
public enum ReviewReportStatus {

    /** 오너 검토 대기 중 */
    PENDING_OWNER_REVIEW,

    /** 오너가 후기 복구 처리 */
    OWNER_RESTORED,

    /** 관리자 검토 요청됨 */
    REQUESTED_ADMIN_REVIEW,

    /** 관리자 승인 (신고 인정) */
    ADMIN_APPROVED,

    /** 관리자 거부 (신고 기각) */
    ADMIN_REJECTED
}
