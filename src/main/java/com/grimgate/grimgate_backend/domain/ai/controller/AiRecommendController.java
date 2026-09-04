package com.grimgate.grimgate_backend.domain.ai.controller;

import com.grimgate.grimgate_backend.domain.ai.dto.AiRecommendRequest;
import com.grimgate.grimgate_backend.domain.ai.dto.AiRecommendResponse;
import com.grimgate.grimgate_backend.domain.ai.service.AiRecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiRecommendController {

    private final AiRecommendService aiRecommendService;

    /**
     * AI-001: 대화 기반 테마 추천
     * POST /api/ai/recommend
     */
    @PostMapping("/recommend")
    public ResponseEntity<AiRecommendResponse> recommend(
            @RequestBody AiRecommendRequest request
    ) {
        return ResponseEntity.ok(aiRecommendService.recommend(request));
    }

    /**
     * AI-002: 랜덤 테마 3개 추천 (페이지 진입 시)
     * GET /api/ai/recommend/random
     */
    @GetMapping("/recommend/random")
    public ResponseEntity<List<AiRecommendResponse.ThemeCard>> random() {
        return ResponseEntity.ok(aiRecommendService.random());
    }
}