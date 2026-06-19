package com.grimgate.grimgate_backend.domain.reservation.repository;

import com.grimgate.grimgate_backend.domain.member.entity.Member;
import com.grimgate.grimgate_backend.domain.reservation.entity.Reservation;
import com.grimgate.grimgate_backend.domain.reservation.entity.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 예약(Reservation) 엔티티에 대한 데이터베이스 액세스 처리를 담당하는 Repository 인터페이스입니다.
 */
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("SELECT r FROM Reservation r JOIN FETCH r.timeSlot ts WHERE r.status = :status AND r.createdAt < :timeLimit")
    List<Reservation> findExpiredReservations(
            @Param("status") ReservationStatus status,
            @Param("timeLimit") LocalDateTime timeLimit
    );

    // 마이페이지 - 회원 기준 예약 목록 조회
    @Query("SELECT r FROM Reservation r JOIN FETCH r.timeSlot ts JOIN FETCH ts.theme t JOIN FETCH t.branch WHERE r.member = :member")
    List<Reservation> findByMemberWithTimeSlot(@Param("member") Member member);

    // 사장님 페이지 - 지점 및 필터 기준 예약 목록 조회
    @Query(value = "SELECT r FROM Reservation r " +
            "JOIN FETCH r.timeSlot ts " +
            "JOIN FETCH ts.theme t " +
            "JOIN FETCH r.member m " +
            "JOIN FETCH m.account a " +
            "WHERE t.branch.id = :branchId " +
            "AND (:startDate IS NULL OR ts.slotDate >= :startDate) " +
            "AND (:endDate IS NULL OR ts.slotDate <= :endDate) " +
            "AND (:themeId IS NULL OR t.id = :themeId) " +
            "AND (:nickname IS NULL OR a.nickname LIKE %:nickname%) " +
            "AND (:status IS NULL OR r.status = :status) " +
            "ORDER BY ts.slotDate ASC, ts.startTime ASC",
            countQuery = "SELECT COUNT(r) FROM Reservation r " +
                    "JOIN r.timeSlot ts " +
                    "JOIN ts.theme t " +
                    "JOIN r.member m " +
                    "JOIN m.account a " +
                    "WHERE t.branch.id = :branchId " +
                    "AND (:startDate IS NULL OR ts.slotDate >= :startDate) " +
                    "AND (:endDate IS NULL OR ts.slotDate <= :endDate) " +
                    "AND (:themeId IS NULL OR t.id = :themeId) " +
                    "AND (:nickname IS NULL OR a.nickname LIKE %:nickname%) " +
                    "AND (:status IS NULL OR r.status = :status)")
    Page<Reservation> findReservationsByBranchAndFilters(
            @Param("branchId") Long branchId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("themeId") Long themeId,
            @Param("nickname") String nickname,
            @Param("status") ReservationStatus status,
            Pageable pageable
    );

    @Query("SELECT " +
            "COUNT(r) as totalCount, " +
            "COALESCE(SUM(CASE WHEN r.timeSlot.slotDate = :today THEN 1 ELSE 0 END), 0) as todayCount, " +
            "COALESCE(SUM(CASE WHEN r.status = com.grimgate.grimgate_backend.domain.reservation.entity.ReservationStatus.COMPLETED THEN 1 ELSE 0 END), 0) as completedCount, " +
            "COALESCE(SUM(CASE WHEN r.status = com.grimgate.grimgate_backend.domain.reservation.entity.ReservationStatus.CONFIRMED THEN 1 ELSE 0 END), 0) as confirmedCount, " +
            "COALESCE(SUM(CASE WHEN r.status = com.grimgate.grimgate_backend.domain.reservation.entity.ReservationStatus.CANCELLED THEN 1 ELSE 0 END), 0) as cancelledCount " +
            "FROM Reservation r " +
            "WHERE r.timeSlot.theme.branch.id = :branchId " +
            "AND (:startDate IS NULL OR r.timeSlot.slotDate >= :startDate) " +
            "AND (:endDate IS NULL OR r.timeSlot.slotDate <= :endDate)")
    ReservationStatsProjection findReservationStats(
            @Param("branchId") Long branchId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("today") LocalDate today
    );
}

