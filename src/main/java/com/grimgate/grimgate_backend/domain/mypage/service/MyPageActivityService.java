package com.grimgate.grimgate_backend.domain.mypage.service;

import com.grimgate.grimgate_backend.domain.mate.repository.MatePostRepository;
import com.grimgate.grimgate_backend.domain.member.entity.Member;
import com.grimgate.grimgate_backend.domain.member.repository.MemberRepository;
import com.grimgate.grimgate_backend.domain.mypage.dto.response.MyPageMatePostResponse;
import com.grimgate.grimgate_backend.domain.mypage.dto.response.MyReviewResponse;
import com.grimgate.grimgate_backend.domain.review.dto.ReviewDeleteResponse;
import com.grimgate.grimgate_backend.domain.review.dto.ReviewResponse;
import com.grimgate.grimgate_backend.domain.review.dto.ReviewUpdateRequest;
import com.grimgate.grimgate_backend.domain.review.entity.Review;
import com.grimgate.grimgate_backend.domain.review.entity.ReviewImage;
import com.grimgate.grimgate_backend.domain.review.repository.ReviewImageRepository;
import com.grimgate.grimgate_backend.domain.review.repository.ReviewRepository;
import com.grimgate.grimgate_backend.domain.theme.entity.Theme;
import com.grimgate.grimgate_backend.domain.theme.repository.ThemeRepository;
import com.grimgate.grimgate_backend.global.S3.S3Uploader;
import com.grimgate.grimgate_backend.global.exception.CustomException;
import com.grimgate.grimgate_backend.global.exception.ErrorCode;
import com.grimgate.grimgate_backend.global.security.SecurityUtil;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageActivityService {

    private final MemberRepository memberRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ThemeRepository themeRepository;
    private final MatePostRepository matePostRepository;
    private final S3Uploader s3Uploader;

    // 내 후기 조회
    public List<MyReviewResponse> getMyReviews(){
        Long accountId = SecurityUtil.getCurrentAccountId();
        Member member = memberRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        return reviewRepository.findByMemberId(member.getId()).stream()
                .map(review -> {
                        List<String> imageUrls = reviewImageRepository.findByReview_Id(review.getId())
                                .stream()
                                .map(ReviewImage::getImageUrl)
                                .toList();

                      return MyReviewResponse.builder()
                              .reviewId(review.getId())
                              .themeTitle(review.getTheme().getTitle())
                              .themeId(review.getTheme().getId())
                              .nickname(review.getMember().getAccount().getNickname())
                        .rating(review.getRating())
                        .horrorRating(review.getHorrorRating())
                        .difficultyRating(review.getDifficultyRating())
                        .tags(review.getTags())
                        .content(review.getContent())
                        .spoiler(review.getSpoiler())
                        .createdAt(review.getCreatedAt())
                              .imageUrls(imageUrls)
                              .visitedAt(review.getReservation() != null ?
                                      review.getReservation().getTimeSlot().getSlotDate().atTime(
                                              review.getReservation().getTimeSlot().getStartTime()
                                      ) : null)
                        .build();
                })
                .toList();
    }

    // 내 후기 수정
    @Transactional
    public ReviewResponse updateMyReview(Long reviewId, ReviewUpdateRequest request, List<MultipartFile> images){
        Long accountId = SecurityUtil.getCurrentAccountId();
        Member member = memberRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getMember().getId().equals(member.getId())) {
            throw new CustomException(ErrorCode.REVIEW_NOT_OWNER);
        }

        // 이미지 최대 3장 검증
        if (images != null && images.size() > 3) {
            throw new CustomException(ErrorCode.IMAGE_LIMIT_EXCEEDED);
        }

        review.update(request);

// 기존 이미지 삭제 후 새로 저장
        reviewImageRepository.deleteByReviewId(reviewId);
        if (images != null && !images.isEmpty()) {
            List<ReviewImage> reviewImages = IntStream.range(0, images.size())
                    .mapToObj(i -> ReviewImage.builder()
                            .review(review)
                            .imageUrl(s3Uploader.upload(images.get(i), "reviews"))
                            .imageOrder(String.valueOf(i + 1))
                            .build())
                    .toList();
            reviewImageRepository.saveAll(reviewImages);
        }

        // theme rating 재계산
        Theme theme = review.getTheme();
        List<Review> allReviews = reviewRepository.findByThemeId(theme.getId());
        double average = allReviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
        double newRating = Math.round(average * 10.0) / 10.0;
        theme.updateRating(newRating, allReviews.size());
        themeRepository.save(theme);

        // 저장된 이미지 조회
        List<String> imageUrls = reviewImageRepository.findByReview_Id(review.getId())
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


    // 내 메이트 모집글 조회
    public List<MyPageMatePostResponse> getMyMatePosts() {
        Long accountId = SecurityUtil.getCurrentAccountId();
        Member member = memberRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        return matePostRepository.findByMemberIdAndDeletedAtIsNull(
                        member.getId(),
                        org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE,
                                Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .map(MyPageMatePostResponse::from)
                .toList();
    }

    //내 후기 삭제
    @Transactional
    public void deleteMyReview(Long reviewId) {
        Long accountId = SecurityUtil.getCurrentAccountId();

        Member member = memberRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        // 본인 확인
        if (!review.getMember().getId().equals(member.getId())) {
            throw new CustomException(ErrorCode.REVIEW_NOT_OWNER);
        }

        // 이미지 먼저 삭제
        reviewImageRepository.deleteByReviewId(reviewId);

        // 후기 삭제
        reviewRepository.delete(review);

        // theme rating 재계산
        Theme theme = review.getTheme();

        List<Review> remaining = reviewRepository.findByThemeId(theme.getId());
        double average = remaining.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
        double newRating = Math.round(average * 10.0) / 10.0;
        theme.updateRating(newRating, remaining.size());
        themeRepository.save(theme);

    }


}
