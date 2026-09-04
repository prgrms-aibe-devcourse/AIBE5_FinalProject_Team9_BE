package com.grimgate.grimgate_backend.domain.admin.dto.response;

import com.grimgate.grimgate_backend.domain.review.entity.Review;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 관리자 후기 목록 조회 응답 DTO
 */
@Getter
@Builder
public class AdminReviewResponse {

    /** 후기 ID */
    private Long reviewId;

    /** 작성자 닉네임 */
    private String memberNickname;

    /** 테마 제목 */
    private String themeTitle;

    /** 별점 */
    private Integer rating;

    /** 후기 본문 */
    private String content;

    /** 후기 상태 (ACTIVE / HIDDEN) */
    private String status;

    /** 스포일러 여부 */
    private Boolean spoiler;

    /** 작성일시 */
    private LocalDateTime createdAt;

    /**
     * Review 엔티티로부터 응답 DTO 생성
     */
    public static AdminReviewResponse from(Review review) {
        return AdminReviewResponse.builder()
                .reviewId(review.getId())
                .memberNickname(review.getMember().getAccount().getNickname())
                .themeTitle(review.getTheme().getTitle())
                .rating(review.getRating())
                .content(review.getContent())
                .status(review.getStatus())
                .spoiler(review.getSpoiler())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
