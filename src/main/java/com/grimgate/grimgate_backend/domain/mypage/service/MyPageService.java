package com.grimgate.grimgate_backend.domain.mypage.service;

import com.grimgate.grimgate_backend.domain.achievement.repository.AchievementRepository;
import com.grimgate.grimgate_backend.domain.achievement.repository.MemberAchievementRepository;
import com.grimgate.grimgate_backend.domain.member.entity.Member;
import com.grimgate.grimgate_backend.domain.member.repository.MemberRepository;
import com.grimgate.grimgate_backend.domain.mypage.dto.request.MyPageProfileUpdateRequest;
import com.grimgate.grimgate_backend.domain.mypage.dto.response.MyPageProfileResponse;
import com.grimgate.grimgate_backend.domain.mypage.dto.response.MyPageReservationResponse;
import com.grimgate.grimgate_backend.domain.mypage.dto.response.MyPageStatsResponse;
import com.grimgate.grimgate_backend.domain.reservation.entity.Reservation;
import com.grimgate.grimgate_backend.domain.reservation.entity.ReservationStatus;
import com.grimgate.grimgate_backend.domain.reservation.repository.ReservationRepository;
import com.grimgate.grimgate_backend.domain.title.repository.TitleRepository;
import com.grimgate.grimgate_backend.domain.title.service.TitleService;
import com.grimgate.grimgate_backend.global.exception.CustomException;
import com.grimgate.grimgate_backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 마이페이지 관련 비즈니스 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;
    private final TitleService titleService;
    private final MemberAchievementRepository memberAchievementRepository;
    private final AchievementRepository achievementRepository;
    private final TitleRepository titleRepository;

    /**
     * 마이페이지 통계 정보 조회 및 칭호 갱신
     */
    @Transactional
    public MyPageStatsResponse getStats(Long accountId) {
        Member member = memberRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        List<Reservation> reservations = reservationRepository.findByMemberWithTimeSlot(member);

        long totalPlayCount = titleService.calcTotalPlayCount(reservations);
        long clearedCount = titleService.calcClearedCount(reservations);
        double successRate = titleService.calcSuccessRate(totalPlayCount, clearedCount);

        // 조건에 맞는 칭호 id가 있으면 업데이트
        Optional<Long> matchingTitleId = titleService.findMatchingTitleId((int) totalPlayCount, (int) clearedCount, successRate);
        matchingTitleId.ifPresent(member::updateTitleId);

        long acquiredAchievementCount = memberAchievementRepository.countByMember_Id(member.getId());
        long totalAchievementCount = achievementRepository.count();

        return MyPageStatsResponse.builder()
                .totalPlayCount((int) totalPlayCount)
                .successRate((int) successRate)
                .bestClearTime(reservations.stream()
                        .filter(r -> (r.getStatus() == ReservationStatus.CONFIRMED || r.getStatus() == ReservationStatus.COMPLETED)
                                && r.getTimeSlot().getSlotDate().isBefore(LocalDate.now())
                                && Boolean.TRUE.equals(r.getIsCleared())
                                && r.getClearTime() != null)
                        .map(Reservation::getClearTime)
                        .min(LocalTime::compareTo)
                        .map(LocalTime::toSecondOfDay)
                        .orElse(null))
                .acquiredAchievementCount(acquiredAchievementCount)
                .totalAchievementCount(totalAchievementCount)
                .build();
    }

    /**
     * 예약 목록 조회 (UPCOMING: 예정, PAST: 지난)
     */
    public List<MyPageReservationResponse> getReservations(Long accountId, String type) {
        if (!"UPCOMING".equals(type) && !"PAST".equals(type)) {
            throw new CustomException(ErrorCode.INVALID_RESERVATION_TYPE);
        }
        Member member = memberRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        List<Reservation> reservations = reservationRepository.findByMemberWithTimeSlot(member);
        LocalDate today = LocalDate.now();

        return reservations.stream()
                .filter(r -> {
                    LocalDate slotDate = r.getTimeSlot().getSlotDate();
                    if ("UPCOMING".equals(type)) {
                        return !slotDate.isBefore(today) && r.getStatus() != ReservationStatus.CANCELLED;
                    }
                    return slotDate.isBefore(today) && r.getStatus() != ReservationStatus.CANCELLED;
                })
                .sorted((a, b) -> "UPCOMING".equals(type)
                        ? a.getTimeSlot().getSlotDate().compareTo(b.getTimeSlot().getSlotDate())
                        : b.getTimeSlot().getSlotDate().compareTo(a.getTimeSlot().getSlotDate()))
                .map(r -> MyPageReservationResponse.builder()
                        .reservationId(r.getId())
                        .themeName(r.getTimeSlot().getTheme().getTitle())
                        .branchName(r.getTimeSlot().getTheme().getBranch().getBranchName())
                        .reservationDate(r.getTimeSlot().getSlotDate())
                        .reservationTime(r.getTimeSlot().getStartTime())
                        .peopleCount(r.getPeopleCount())
                        .status(r.getStatus().name())
                        .isCleared("UPCOMING".equals(type) ? null : r.getIsCleared())
                        .clearTime(r.getClearTime() == null ? null : r.getClearTime().toSecondOfDay())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 프로필 수정
     */
    @Transactional
    public void updateProfile(Long accountId, MyPageProfileUpdateRequest request) {
        Member member = memberRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        member.getAccount().updateProfile(
                request.getNickname(),
                request.getAge(),
                request.getGender(),
                request.getAgeVisible(),
                request.getGenderVisible(),
                request.getEmailVisible()
        );
    }

    /**
     * 마이페이지 프로필 정보 조회
     */
    public MyPageProfileResponse getProfile(Long accountId) {
        Member member = memberRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 칭호명 조회 (없으면 null)
        String titleName = null;
        if (member.getTitleId() != null) {
            titleName = titleRepository.findById(member.getTitleId())
                    .map(title -> title.getName())
                    .orElse(null);
        }

        // 성별 공개 여부 확인
        String gender = member.getAccount().isGenderVisible() ? member.getAccount().getGender() : null;

        // 나이 공개 여부 확인
        Integer age = member.getAccount().isAgeVisible() ? member.getAccount().getAge() : null;

        return MyPageProfileResponse.builder()
                .nickname(member.getAccount().getNickname())
                .titleName(titleName)
                .gender(gender)
                .age(age)
                .profileCharacterImageUrl(
                        member.getProfileCharacter() != null
                                ? member.getProfileCharacter().getImageUrl()
                                : null
                )
                .build();
    }
}
