package com.grimgate.grimgate_backend.domain.review.controller;

import com.grimgate.grimgate_backend.domain.review.dto.ReviewReportCreateRequest;
import com.grimgate.grimgate_backend.domain.review.dto.ReviewResponse;
import com.grimgate.grimgate_backend.domain.review.dto.ReviewTabResponse;
import com.grimgate.grimgate_backend.domain.review.service.ReviewReportService;
import com.grimgate.grimgate_backend.domain.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewReportService reviewReportService;

    //전체테마 후기 조회
    @GetMapping("/themes/{themeId}/reviews")
    public ReviewTabResponse getReviewsByThemeId(
            @PathVariable Long themeId,
            @RequestParam(required = false, defaultValue = "1")
            Integer page,

            @RequestParam(required = false, defaultValue = "10")
            Integer limit,

            @RequestParam(required = false, defaultValue = "latest")
            String sort

            //1번째 페이지, 10개 조회, 최신순 정렬
    ) {

        return reviewService.getReviewTab(
                themeId, page, limit, sort);
    }

    //후기 단건 상세 조회
    @GetMapping("/reviews/{reviewId}")
    public ReviewResponse getReviewById(
            @PathVariable Long reviewId){
        return reviewService.getReviewById(reviewId);
    }

    //후기 신고 접수
    @PostMapping("/reviews/{reviewId}/reports")
    public ResponseEntity<Void> createReport(
            @PathVariable Long reviewId,
            @RequestBody @Valid ReviewReportCreateRequest request) {
        reviewReportService.createReport(reviewId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
