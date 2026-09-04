package com.grimgate.grimgate_backend.domain.admin.dto.response;

import com.grimgate.grimgate_backend.domain.review.entity.Review;
import com.grimgate.grimgate_backend.domain.review.entity.ReviewImage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 후기 상세 조회 응답 DTO
 */
@Getter
@Builder
public class AdminReviewDetailResponse {

    /** 후기 ID */
    private Long reviewId;

    /** 별점 */
    private Integer rating;

    /** 공포 별점 */
    private Integer horrorRating;

    /** 난이도 별점 */
    private Integer difficultyRating;

    /** 후기 본문 */
    private String content;

    /** 태그 */
    private String tags;

    /** 스포일러 여부 */
    private Boolean spoiler;

    /** 후기 상태 (ACTIVE / HIDDEN) */
    private String status;

    /** 작성자 닉네임 */
    private String memberNickname;

    /** 작성자 이메일 */
    private String memberEmail;

    /** 테마 ID */
    private Long themeId;

    /** 테마 제목 */
    private String themeTitle;

    /** 작성일시 */
    private LocalDateTime createdAt;

    /** 수정일시 */
    private LocalDateTime updatedAt;

    /** 삭제일시 */
    private LocalDateTime deletedAt;

    /** 후기 이미지 URL 목록 */
    private List<String> imageUrls;

    /**
     * Review 엔티티와 이미지 목록으로부터 응답 DTO 생성
     */
    public static AdminReviewDetailResponse from(Review review, List<ReviewImage> images) {
        return AdminReviewDetailResponse.builder()
                .reviewId(review.getId())
                .rating(review.getRating())
                .horrorRating(review.getHorrorRating())
                .difficultyRating(review.getDifficultyRating())
                .content(review.getContent())
                .tags(review.getTags())
                .spoiler(review.getSpoiler())
                .status(review.getStatus())
                .memberNickname(review.getMember().getAccount().getNickname())
                .memberEmail(review.getMember().getAccount().getEmail())
                .themeId(review.getTheme().getId())
                .themeTitle(review.getTheme().getTitle())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .deletedAt(review.getDeletedAt())
                .imageUrls(images.stream()
                        .map(ReviewImage::getImageUrl)
                        .toList())
                .build();
    }
}
