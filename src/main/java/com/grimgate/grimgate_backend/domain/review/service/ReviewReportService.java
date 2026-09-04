package com.grimgate.grimgate_backend.domain.review.service;

import com.grimgate.grimgate_backend.domain.manager.entity.Manager;
import com.grimgate.grimgate_backend.domain.manager.repository.ManagerRepository;
import com.grimgate.grimgate_backend.domain.member.entity.Member;
import com.grimgate.grimgate_backend.domain.member.repository.MemberRepository;
import com.grimgate.grimgate_backend.domain.owner.dto.OwnerReviewReportResponse;
import com.grimgate.grimgate_backend.domain.review.dto.ReviewReportCreateRequest;
import com.grimgate.grimgate_backend.domain.owner.dto.ReviewReportHideRequest;
import com.grimgate.grimgate_backend.domain.review.entity.ReviewReportStatus;
import com.grimgate.grimgate_backend.domain.review.entity.Review;
import com.grimgate.grimgate_backend.domain.review.entity.ReviewReport;
import com.grimgate.grimgate_backend.domain.review.repository.ReviewReportRepository;
import com.grimgate.grimgate_backend.domain.review.repository.ReviewRepository;
import com.grimgate.grimgate_backend.global.exception.CustomException;
import com.grimgate.grimgate_backend.global.exception.ErrorCode;
import com.grimgate.grimgate_backend.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 후기 신고 서비스
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewReportService {

    private final ReviewReportRepository reviewReportRepository;
    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;
    private final ManagerRepository managerRepository;

    // 후기 신고 접수
    public void createReport(Long reviewId, ReviewReportCreateRequest request) {
        // 1. 로그인 사용자 accountId 추출
        Long accountId = SecurityUtil.getCurrentAccountId();

        // 2. Member 조회
        Member reporter = memberRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 3. Review 조회
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        // 4. 동일 유저 중복 신고 체크
        if (reviewReportRepository.existsByReview_IdAndReporter_Id(reviewId, reporter.getId())) {
            throw new CustomException(ErrorCode.REVIEW_REPORT_ALREADY_EXISTS);
        }

        // 5. 신고 엔티티 생성
        ReviewReport report = ReviewReport.create(review, reporter, request.getReason(), request.getDetail());

        // 6. 저장
        reviewReportRepository.save(report);
    }

    // 사장님 후기 복구 처리
    public void restoreByOwner(Long reportId) {
        // 1. 로그인 사용자 accountId 추출
        Long accountId = SecurityUtil.getCurrentAccountId();

        // 2. Manager 조회
        Manager manager = managerRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.MANAGER_NOT_FOUND));

        // 3. 신고 내역 조회
        ReviewReport report = reviewReportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_REPORT_NOT_FOUND));

        // 4. 소유권 검증 (자기 지점 신고인지 확인)
        validateOwnerAccess(manager, report);

        // 5. 상태 전이 검증 (PENDING_OWNER_REVIEW 상태에서만 허용)
        validateOwnerActionStatus(report);

        // 6. 복구 처리 (review.status는 변경하지 않음)
        report.restoreByOwner(manager);
    }

    // 사장님 자기 지점 후기 신고 목록 조회
    @Transactional(readOnly = true)
    public Page<OwnerReviewReportResponse> getReportsByOwner(int page, int limit) {
        // 1. 로그인 사용자 accountId 추출
        Long accountId = SecurityUtil.getCurrentAccountId();

        // 2. Manager 조회
        Manager manager = managerRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.MANAGER_NOT_FOUND));

        // 3. 자기 지점 신고만 조회 (branch.managerId = manager.id)
        return reviewReportRepository
                .findByReview_Theme_Branch_ManagerId(manager.getId(), PageRequest.of(page, limit))
                .map(OwnerReviewReportResponse::from);
    }

    // 사장님 관리자 검토 요청 (숨김 처리)
    public void requestHideByOwner(Long reportId, ReviewReportHideRequest request) {
        // 1. 로그인 사용자 accountId 추출
        Long accountId = SecurityUtil.getCurrentAccountId();

        // 2. Manager 조회
        Manager manager = managerRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.MANAGER_NOT_FOUND));

        // 3. 신고 내역 조회
        ReviewReport report = reviewReportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_REPORT_NOT_FOUND));

        // 4. 소유권 검증 (자기 지점 신고인지 확인)
        validateOwnerAccess(manager, report);

        // 5. 상태 전이 검증 (PENDING_OWNER_REVIEW 상태에서만 허용)
        validateOwnerActionStatus(report);

        // 6. 관리자 검토 요청 처리 (review.status는 관리자 승인 전까지 ACTIVE 유지)
        report.requestHideByOwner(manager, request.getOwnerReason());
    }

    // 신고가 로그인한 사장님 소유 지점의 것인지 검증
    private void validateOwnerAccess(Manager manager, ReviewReport report) {
        Long branchManagerId = report.getReview().getTheme().getBranch().getManagerId();
        if (!branchManagerId.equals(manager.getId())) {
            throw new CustomException(ErrorCode.BRANCH_ACCESS_DENIED);
        }
    }

    // 사장님 처리 가능 상태 검증 (PENDING_OWNER_REVIEW 상태에서만 허용)
    private void validateOwnerActionStatus(ReviewReport report) {
        if (report.getStatus() != ReviewReportStatus.PENDING_OWNER_REVIEW) {
            throw new CustomException(ErrorCode.INVALID_REVIEW_REPORT_STATUS);
        }
    }
}
