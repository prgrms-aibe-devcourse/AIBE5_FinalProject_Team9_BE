package com.grimgate.grimgate_backend.domain.review.repository;

import com.grimgate.grimgate_backend.domain.review.entity.Review;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

// 관리자 후기 검색/필터 조건 Specification 모음
public class ReviewSpecification {

    // 후기 상태 필터 (ACTIVE / HIDDEN)
    public static Specification<Review> statusEquals(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    // 특정 테마 필터
    public static Specification<Review> themeIdEquals(Long themeId) {
        if (themeId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("theme").get("id"), themeId);
    }

    // 작성일 범위 필터
    public static Specification<Review> createdAtBetween(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom == null && dateTo == null) {
            return null;
        }
        if (dateFrom != null && dateTo == null) {
            return (root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom.atStartOfDay());
        }
        if (dateFrom == null) {
            return (root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("createdAt"), dateTo.atTime(23, 59, 59));
        }
        return (root, query, cb) -> cb.between(
                root.get("createdAt"),
                dateFrom.atStartOfDay(),
                dateTo.atTime(23, 59, 59)
        );
    }

    // 후기 본문 또는 작성자 닉네임 키워드 검색
    public static Specification<Review> keywordContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String likePattern = "%" + keyword + "%";
        return (root, query, cb) -> {
            // Review → Member → Account join
            Join<Object, Object> member = root.join("member");
            Join<Object, Object> account = member.join("account");

            return cb.or(
                    cb.like(root.get("content"), likePattern),
                    cb.like(account.get("nickname"), likePattern)
            );
        };
    }
}
