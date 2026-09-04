package com.grimgate.grimgate_backend.domain.admin.controller;

import com.grimgate.grimgate_backend.domain.admin.dto.request.AdminReviewDecisionRequest;
import com.grimgate.grimgate_backend.domain.admin.dto.request.AdminReviewSearchRequest;
import com.grimgate.grimgate_backend.domain.admin.dto.response.AdminReviewDetailResponse;
import com.grimgate.grimgate_backend.domain.admin.dto.response.AdminReviewResponse;
import com.grimgate.grimgate_backend.domain.admin.dto.response.AdminReviewReportResponse;
import com.grimgate.grimgate_backend.domain.admin.dto.response.AdminReviewStatsResponse;
import com.grimgate.grimgate_backend.domain.admin.service.AdminReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 관리자 후기 신고 처리 컨트롤러
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    // 관리자 후기 목록 조회 (검색/필터 조건 기반)
    @GetMapping("/reviews")
    public ResponseEntity<Page<AdminReviewResponse>> getReviews(
            @ModelAttribute AdminReviewSearchRequest request) {
        return ResponseEntity.ok(adminReviewService.getReviews(request));
    }

    // 관리자 후기 통계 조회
    @GetMapping("/reviews/stats")
    public ResponseEntity<AdminReviewStatsResponse> getStats() {
        return ResponseEntity.ok(adminReviewService.getStats());
    }

    // 관리자 후기 목록 엑셀 다운로드 (검색/필터 조건 기반)
    @GetMapping("/reviews/export")
    public ResponseEntity<byte[]> exportReviews(
            @ModelAttribute AdminReviewSearchRequest request) {
        byte[] excelBytes = adminReviewService.exportReviews(request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "admin-reviews.xlsx");
        headers.setContentLength(excelBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelBytes);
    }

    // 관리자 후기 상세 조회
    @GetMapping("/reviews/{reviewId}")
    public ResponseEntity<AdminReviewDetailResponse> getReviewDetail(@PathVariable Long reviewId) {
        return ResponseEntity.ok(adminReviewService.getReviewDetail(reviewId));
    }

    // 관리자 검토 요청된 신고 목록 조회
    @GetMapping("/review-reports")
    public ResponseEntity<Page<AdminReviewReportResponse>> getReviewReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "16") int limit) {
        return ResponseEntity.ok(adminReviewService.getReviewReports(page, limit));
    }

    // 관리자 후기 신고 승인
    @PatchMapping("/review-reports/{reportId}/approve")
    public ResponseEntity<Void> approveReport(
            @PathVariable Long reportId,
            @RequestBody @Valid AdminReviewDecisionRequest request) {
        adminReviewService.approveReport(reportId, request);
        return ResponseEntity.ok().build();
    }

    // 관리자 후기 신고 반려
    @PatchMapping("/review-reports/{reportId}/reject")
    public ResponseEntity<Void> rejectReport(
            @PathVariable Long reportId,
            @RequestBody @Valid AdminReviewDecisionRequest request) {
        adminReviewService.rejectReport(reportId, request);
        return ResponseEntity.ok().build();
    }
}
