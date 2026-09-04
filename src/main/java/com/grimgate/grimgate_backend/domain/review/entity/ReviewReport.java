package com.grimgate.grimgate_backend.domain.review.entity;

import com.grimgate.grimgate_backend.domain.account.entity.Account;
import com.grimgate.grimgate_backend.domain.manager.entity.Manager;
import com.grimgate.grimgate_backend.domain.member.entity.Member;
import com.grimgate.grimgate_backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 후기 신고 엔티티
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "review_report")
public class ReviewReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 신고 대상 후기 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    /** 신고자 (일반 회원) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private Member reporter;

    /** 신고 사유 (선택지) */
    @Column(nullable = false)
    private String reason;

    /** 신고 상세 내용 */
    @Column(columnDefinition = "TEXT")
    private String detail;

    /** 신고 처리 상태 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewReportStatus status;

    /** 처리한 오너 (방탈출 업체 매니저) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Manager owner;

    /** 오너 처리 사유 */
    @Column(name = "owner_reason", columnDefinition = "TEXT")
    private String ownerReason;

    /** 오너 처리 일시 */
    @Column(name = "owner_handled_at")
    private LocalDateTime ownerHandledAt;

    /** 처리한 관리자 계정 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private Account admin;

    /** 관리자 처리 사유 */
    @Column(name = "admin_reason", columnDefinition = "TEXT")
    private String adminReason;

    /** 관리자 처리 일시 */
    @Column(name = "admin_handled_at")
    private LocalDateTime adminHandledAt;

    /** 최종 해결 일시 */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /** 오너가 후기를 복구 처리 */
    public void restoreByOwner(Manager owner) {
        this.owner = owner;
        this.ownerHandledAt = LocalDateTime.now();
        this.status = ReviewReportStatus.OWNER_RESTORED;
    }

    /** 오너가 관리자 검토를 요청하며 숨김 처리 */
    public void requestHideByOwner(Manager owner, String ownerReason) {
        this.owner = owner;
        this.ownerReason = ownerReason;
        this.ownerHandledAt = LocalDateTime.now();
        this.status = ReviewReportStatus.REQUESTED_ADMIN_REVIEW;
    }

    /** 관리자가 신고를 승인 처리 (신고 인정) */
    public void approveByAdmin(Account admin, String adminReason) {
        this.admin = admin;
        this.adminReason = adminReason;
        this.adminHandledAt = LocalDateTime.now();
        this.resolvedAt = LocalDateTime.now();
        this.status = ReviewReportStatus.ADMIN_APPROVED;
    }

    /** 관리자가 신고를 반려 처리 (신고 기각) */
    public void rejectByAdmin(Account admin, String adminReason) {
        this.admin = admin;
        this.adminReason = adminReason;
        this.adminHandledAt = LocalDateTime.now();
        this.resolvedAt = LocalDateTime.now();
        this.status = ReviewReportStatus.ADMIN_REJECTED;
    }

    /** 신고 생성 팩토리 메서드 */
    public static ReviewReport create(Review review, Member reporter, String reason, String detail) {
        ReviewReport report = new ReviewReport();
        report.review = review;
        report.reporter = reporter;
        report.reason = reason;
        report.detail = detail;
        report.status = ReviewReportStatus.PENDING_OWNER_REVIEW;
        return report;
    }
}
