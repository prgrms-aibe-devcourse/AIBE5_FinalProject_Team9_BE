package com.grimgate.grimgate_backend.domain.review.service;

import com.grimgate.grimgate_backend.domain.review.dto.ReviewResponse;
import com.grimgate.grimgate_backend.domain.review.dto.ReviewTabResponse;
import com.grimgate.grimgate_backend.domain.review.entity.Review;
import com.grimgate.grimgate_backend.domain.review.entity.ReviewImage;
import com.grimgate.grimgate_backend.domain.review.repository.ReviewImageRepository;
import com.grimgate.grimgate_backend.domain.review.repository.ReviewRepository;
import com.grimgate.grimgate_backend.domain.theme.entity.Theme;
import com.grimgate.grimgate_backend.domain.theme.repository.ThemeRepository;
import com.grimgate.grimgate_backend.global.exception.CustomException;
import com.grimgate.grimgate_backend.global.exception.ErrorCode;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ThemeRepository themeRepository;
    private final ReviewImageRepository reviewImageRepository;

    public ReviewTabResponse getReviewTab(Long themeId, Integer page, Integer limit, String sort) {

        // 1. Theme에서 총 개수, 평균 별점 가져오기
        Theme theme = themeRepository.findById(themeId)
                .orElseThrow(() -> new EntityNotFoundException("테마를 찾을 수 없습니다."));

        // 2. 별점 분포 계산 (전체 후기 기준)
        List<Review> allReviews = reviewRepository.findByThemeIdAndStatus(themeId, "ACTIVE");
        Map<Integer, Integer> distribution = new LinkedHashMap<>();
        for (int i = 5; i >= 1; i--) {
            int star = i;
            long count = allReviews.stream().filter(r -> r.getRating() == star).count();
            int percent = allReviews.size() > 0
                    ? (int) Math.round((double) count / allReviews.size() * 100)
                    : 0;
            distribution.put(star, percent);
        }

        // 3. 정렬 + 페이지네이션 적용한 후기 목록
        Pageable pageable = buildPageable(page, limit, sort);
        List<ReviewResponse> reviews = reviewRepository.findByThemeIdAndStatus(themeId, "ACTIVE", pageable)
                .stream()
                .map(review -> ReviewResponse.builder()
                        .id(review.getId())
                        .nickname(review.getMember().getAccount().getNickname())
                        .rating(review.getRating())
                        .horrorRating(review.getHorrorRating())
                        .difficultyRating(review.getDifficultyRating())
                        .tags(review.getTags())
                        .content(review.getContent())
                        .spoiler(review.getSpoiler())
                        .createdAt(review.getCreatedAt())
                        .build())
                .toList();

        // 4. 조합해서 반환
        double rating = theme.getRating() != null ? theme.getRating() : 0.0;

        return new ReviewTabResponse(
                rating,
                allReviews.size(),
                theme.getMinPeople(),
                theme.getMaxPeople(),
                theme.getPlayTime(),
                theme.getThumbnailUrl(),
                rating, //평균
                distribution,
                reviews
        );
    }

    private Pageable buildPageable(Integer page, Integer limit, String sort) {
        Sort sorting = sort.equals("rating")
                ? Sort.by(Sort.Direction.DESC, "rating")
                : Sort.by(Sort.Direction.DESC, "createdAt");
        return PageRequest.of(page - 1, limit, sorting);
    }

    public ReviewResponse getReviewById(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        List<String> imageUrls = reviewImageRepository.findByReview_Id(reviewId)
                .stream()
                .map(ReviewImage::getImageUrl)
                .toList();

        return ReviewResponse.builder()
                .nickname(review.getMember().getAccount().getNickname())
                .rating(review.getRating())
                .horrorRating(review.getHorrorRating())
                .difficultyRating(review.getDifficultyRating())
                .tags(review.getTags())
                .content(review.getContent())
                .spoiler(review.getSpoiler())
                .createdAt(review.getCreatedAt())
                .imageUrls(imageUrls)
                .build();
    }

}
