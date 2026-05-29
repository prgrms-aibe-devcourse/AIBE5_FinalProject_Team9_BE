package com.grimgate.grimgate_backend.domain.review.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class ReviewTabResponse {
    private int reviewCount;        // theme.getReviewCount()
    private double averageRating;  // theme.getRating()
    private List<ReviewResponse> reviews;  // 기존 ReviewResponse 재사용
    private Map<Integer, Integer> ratingDistribution; // 별점 분포도
}
