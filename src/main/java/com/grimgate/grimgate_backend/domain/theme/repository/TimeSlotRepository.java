package com.grimgate.grimgate_backend.domain.theme.repository;

import com.grimgate.grimgate_backend.domain.theme.entity.TimeSlot;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
