package com.grimgate.grimgate_backend.domain.mypage.service;

import com.grimgate.grimgate_backend.domain.member.entity.Member;
import com.grimgate.grimgate_backend.domain.member.repository.MemberRepository;
import com.grimgate.grimgate_backend.domain.reservation.entity.Reservation;
import com.grimgate.grimgate_backend.domain.reservation.entity.ReservationStatus;
import com.grimgate.grimgate_backend.domain.reservation.repository.ReservationRepository;
import com.grimgate.grimgate_backend.domain.review.dto.ReviewCreateRequest;
import com.grimgate.grimgate_backend.domain.review.dto.ReviewResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional
public class MyPageReservationService {

    private final ReservationRepository reservationRepository;
    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ThemeRepository themeRepository;
    private final S3Uploader s3Uploader;


    //후기 작성
    public ReviewResponse createReview(ReviewCreateRequest request, List<MultipartFile> images) {
        Long accountId = SecurityUtil.getCurrentAccountId();

        Member member = memberRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Reservation reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESERVATION_NOT_FOUND));

        //본인 예약인지 확인
        if (!reservation.getMember().getId().equals(member.getId())) {
            throw new CustomException(ErrorCode.REVIEW_NOT_OWNER);
        }
        //지난 예약인지 확인
        if (!reservation.getTimeSlot().getSlotDate().isBefore(LocalDate.now())) {
            throw new CustomException(ErrorCode.RESERVATION_NOT_COMPLETED);
        }
        //예약 상태 확인
        boolean isPast = reservation.getTimeSlot().getSlotDate().isBefore(LocalDate.now());
        boolean isCompleted = reservation.getStatus() == ReservationStatus.COMPLETED;
        boolean isConfirmedAndPast = reservation.getStatus() == ReservationStatus.CONFIRMED && isPast;

        if (!isCompleted && !isConfirmedAndPast) {
            throw new CustomException(ErrorCode.RESERVATION_NOT_COMPLETED);
        }
        //중복 후기 확인
        if (reviewRepository.existsByReservationId(request.getReservationId())) {
            throw new CustomException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Theme theme = reservation.getTimeSlot().getTheme();

        // 이미지 최대 3장 검증
        if (images!= null && images.size() > 3) {
            throw new CustomException(ErrorCode.IMAGE_LIMIT_EXCEEDED);
        }

        Review review = Review.create(member, theme, reservation, request);
        reviewRepository.save(review);

        // theme rating 재계산
        List<Review> allReviews = reviewRepository.findByThemeId(theme.getId());
        double average = allReviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
        double newRating = Math.round(average * 10.0) / 10.0;
        theme.updateRating(newRating, allReviews.size());
        themeRepository.save(theme);

        // 이미지 저장
        if (images != null && !images.isEmpty() ) {
            List<ReviewImage> reviewImages = IntStream.range(0, images.size())
                    .mapToObj(i -> ReviewImage.builder()
                            .review(review)
                            .imageUrl(s3Uploader.upload(images.get(i), "reviews"))
                            .imageOrder(String.valueOf(i + 1))
                            .build())
                    .toList();
            reviewImageRepository.saveAll(reviewImages);
        }

        // 저장된 이미지 조회
        List<String> imageUrls = reviewImageRepository.findByReview_Id(review.getId())
                .stream()
                .map(ReviewImage::getImageUrl)
                .toList();

        return ReviewResponse.builder()
                .nickname(member.getAccount().getNickname())
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
