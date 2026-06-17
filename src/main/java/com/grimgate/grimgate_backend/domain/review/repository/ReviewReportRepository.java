package com.grimgate.grimgate_backend.domain.review.repository;

import com.grimgate.grimgate_backend.domain.review.entity.ReviewReport;
import com.grimgate.grimgate_backend.domain.review.entity.ReviewReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

    // 처리 상태별 신고 목록 조회 (관리자 숨김 요청 목록 페이징)
    Page<ReviewReport> findByStatus(ReviewReportStatus status, Pageable pageable);

    // 동일 유저의 동일 후기 중복 신고 여부 확인
    boolean existsByReview_IdAndReporter_Id(Long reviewId, Long reporterId);

    // 관리자 통계 — 처리 상태별 신고 수 집계
    long countByStatus(ReviewReportStatus status);

    // 사장님 자기 지점 후기 신고 목록 조회 (branch.managerId 기준 필터)
    Page<ReviewReport> findByReview_Theme_Branch_ManagerId(Long managerId, Pageable pageable);

    Page<ReviewReport> findByStatusIn(List<ReviewReportStatus> statuses, Pageable pageable);
}
