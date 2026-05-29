package com.grimgate.grimgate_backend.domain.review.controller;

import com.grimgate.grimgate_backend.domain.review.dto.ReviewTabResponse;
import com.grimgate.grimgate_backend.domain.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/themes")
public class ReviewController {

    private final ReviewService reviewService;

    //조회
    @GetMapping("/{themeId}/reviews")
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
}
