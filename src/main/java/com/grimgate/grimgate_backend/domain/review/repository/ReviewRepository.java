package com.grimgate.grimgate_backend.domain.review.repository;

import com.grimgate.grimgate_backend.domain.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {
    List<Review> findByThemeId(Long themeId, Pageable pageable);
    List<Review> findByThemeId(Long themeId);
    // 내가 쓴 후기 목록(마이페이지)
    List<Review> findByMemberId(Long memberId);
    // 예약에 이미 후기 있는지 확인 (중복 방지)
    boolean existsByReservationId(Long reservationId);
    void deleteByThemeId(Long themeId);
    // 관리자 후기 목록 조회 (status 필터 + 페이징)
    Page<Review> findByStatus(String status, Pageable pageable);

    // 관리자 통계 — status별 후기 수 집계
    long countByStatus(String status);

    //N+1로 fetch join 쿼리 하나로 다 긁어오기
    @Query("SELECT DISTINCT r FROM Review r " +
            "LEFT JOIN FETCH r.images " +
            "LEFT JOIN FETCH r.theme " +
            "LEFT JOIN FETCH r.reservation res " +
            "LEFT JOIN FETCH res.timeSlot " +
            "WHERE r.member.id = :memberId")
    List<Review> findByMemberIdWithDetails(@Param("memberId") Long memberId);
}
