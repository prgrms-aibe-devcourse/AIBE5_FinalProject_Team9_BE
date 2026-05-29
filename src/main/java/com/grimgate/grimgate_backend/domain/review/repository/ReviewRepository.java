package com.grimgate.grimgate_backend.domain.review.repository;

import com.grimgate.grimgate_backend.domain.review.entity.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByThemeId(Long themeId, Pageable pageable);
    List<Review> findByThemeId(Long themeId);
}
