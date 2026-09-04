package com.grimgate.grimgate_backend.domain.title.service;

import com.grimgate.grimgate_backend.domain.reservation.entity.Reservation;
import com.grimgate.grimgate_backend.domain.reservation.entity.ReservationStatus;
import com.grimgate.grimgate_backend.domain.title.entity.Title;
import com.grimgate.grimgate_backend.domain.title.repository.TitleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 회원의 예약 이력을 기반으로 칭호 조건을 계산하는 컴포넌트
 */
@Component
@RequiredArgsConstructor
public class TitleService {

    private final TitleRepository titleRepository;

    /**
     * 실제로 플레이 완료된 예약 수 계산
     * 조건: status == CONFIRMED AND slotDate가 오늘 이전
     */
    public long calcTotalPlayCount(List<Reservation> reservations) {
        return reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED || r.getStatus() == ReservationStatus.COMPLETED)
                .filter(r -> r.getTimeSlot().getSlotDate().isBefore(LocalDate.now()))
                .count();
    }

    /**
     * 클리어 성공 횟수 계산
     * 조건: calcTotalPlayCount 조건 + isCleared == true
     */
    public long calcClearedCount(List<Reservation> reservations) {
        return reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED || r.getStatus() == ReservationStatus.COMPLETED)
                .filter(r -> r.getTimeSlot().getSlotDate().isBefore(LocalDate.now()))
                .filter(r -> Boolean.TRUE.equals(r.getIsCleared()))
                .count();
    }

    /**
     * 성공률 계산 (0~100)
     */
    public double calcSuccessRate(long totalPlayCount, long clearedCount) {
        if (totalPlayCount == 0) {
            return 0.0;
        }
        return (double) clearedCount / totalPlayCount * 100;
    }

    /**
     * 조건에 맞는 칭호 ID 반환
     */
    public Optional<Long> findMatchingTitleId(int totalPlayCount, int clearedCount, double successRate) {
        List<Title> titles = titleRepository.findAll();

        return titles.stream()
                .filter(title -> title.getMinSuccessRate() != null && title.getMaxSuccessRate() != null)
                .filter(title -> title.getMinSuccessRate() <= successRate && successRate <= title.getMaxSuccessRate())
                .filter(title -> {
                    if (title.getRequiredClearCount() != null) {
                        return clearedCount >= title.getRequiredClearCount();
                    }
                    return true;
                })
                .max(Comparator.comparingDouble(Title::getMinSuccessRate))
                .map(Title::getId);
    }
}
