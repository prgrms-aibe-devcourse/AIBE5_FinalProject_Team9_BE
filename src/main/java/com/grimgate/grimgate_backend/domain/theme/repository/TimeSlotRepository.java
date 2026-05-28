package com.grimgate.grimgate_backend.domain.theme.repository;

import com.grimgate.grimgate_backend.domain.theme.entity.TimeSlot;
import com.grimgate.grimgate_backend.domain.theme.entity.TimeSlotStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 타임슬롯(TimeSlot) 엔티티에 대한 데이터베이스 액세스 처리를 담당하는 Repository 인터페이스입니다.
 */
@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    /**
     * 특정 테마의 특정 날짜에 해당하는 모든 타임슬롯을 조회합니다.
     *
     * @param themeId  테마 식별자
     * @param slotDate 조회할 날짜
     * @return 타임슬롯 엔티티 목록
     */
    List<TimeSlot> findByThemeIdAndSlotDateOrderByStartTimeAsc(Long themeId, LocalDate slotDate);

    /**
     * 빠른예약 페이지에서 필터 조건에 부합하는 예약 가능한 타임슬롯 목록을 조회합니다.
     * N+1 문제를 방지하기 위해 테마(Theme)와 지점(Branch)을 JOIN FETCH로 한 번에 조회합니다.
     *
     * [이 쿼리가 필요한 이유]
     * - 빠른예약의 실시간 조건 검색 성능 최적화를 위해 다수의 필터 조건을 단일 쿼리 레벨에서 처리합니다.
     * - JOIN FETCH를 적용하지 않으면 조회된 타임슬롯 수만큼 추가적인 테마/지점 조회 쿼리가 발생하여 DB 부하가 심각해질 수 있습니다.
     */
    @Query("SELECT ts FROM TimeSlot ts " +
           "JOIN FETCH ts.theme t " +
           "JOIN FETCH t.branch b " +
           "WHERE ts.status = :status " +
           "AND ts.slotDate BETWEEN :dateFrom AND :dateTo " +
           "AND (:region IS NULL OR b.region = :region) " +
           "AND (:peopleCount IS NULL OR (t.minPeople <= :peopleCount AND t.maxPeople >= :peopleCount)) " +
           "AND (:horrorLevel IS NULL OR t.horrorLevel = :horrorLevel) " +
           "AND (:difficulty IS NULL OR t.difficulty = :difficulty) " +
           "AND (:minRating IS NULL OR t.rating >= :minRating)")
    List<TimeSlot> findAvailableSlotsWithFilters(
            @Param("status") TimeSlotStatus status,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("region") String region,
            @Param("peopleCount") Integer peopleCount,
            @Param("horrorLevel") Integer horrorLevel,
            @Param("difficulty") Integer difficulty,
            @Param("minRating") Double minRating
    );
}
