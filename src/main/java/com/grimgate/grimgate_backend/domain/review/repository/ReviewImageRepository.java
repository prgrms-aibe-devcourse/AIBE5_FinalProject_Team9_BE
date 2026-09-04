package com.grimgate.grimgate_backend.domain.review.repository;

import com.grimgate.grimgate_backend.domain.review.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {
    void deleteByReviewId(Long reviewId);
    List<ReviewImage> findByReview_Id(Long reviewId);

}
