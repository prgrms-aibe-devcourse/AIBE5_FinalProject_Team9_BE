package com.grimgate.grimgate_backend.domain.achievement.service;

import com.grimgate.grimgate_backend.domain.achievement.entity.Achievement;
import com.grimgate.grimgate_backend.domain.achievement.entity.AchievementConditionType;
import com.grimgate.grimgate_backend.domain.achievement.entity.MemberAchievement;
import com.grimgate.grimgate_backend.domain.achievement.repository.AchievementRepository;
import com.grimgate.grimgate_backend.domain.achievement.repository.MemberAchievementRepository;
import com.grimgate.grimgate_backend.domain.member.entity.Member;
import com.grimgate.grimgate_backend.domain.member.repository.MemberRepository;
import com.grimgate.grimgate_backend.domain.mypage.dto.response.MyPageAchievementResponse;
import com.grimgate.grimgate_backend.domain.achievement.dto.AchievementGrantResult;
import com.grimgate.grimgate_backend.global.exception.CustomException;
import com.grimgate.grimgate_backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 업적 관련 비즈니스 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AchievementService {

    private final MemberRepository memberRepository;
    private final AchievementRepository achievementRepository;
    private final MemberAchievementRepository memberAchievementRepository;

    /**
     * 업적 목록 조회 (전체 업적 기준, 획득 여부 포함)
     */
    public List<MyPageAchievementResponse> getAchievements(Long accountId) {
        Member member = memberRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        List<Achievement> allAchievements = achievementRepository.findAll();
        List<MemberAchievement> memberAchievements = memberAchievementRepository.findByMember_Id(member.getId());

        // 획득한 업적 ID → MemberAchievement 맵
        Map<Long, MemberAchievement> acquiredMap = memberAchievements.stream()
                .collect(Collectors.toMap(ma -> ma.getAchievement().getId(), ma -> ma));

        return allAchievements.stream()
                .map(achievement -> {
                    MemberAchievement ma = acquiredMap.get(achievement.getId());
                    return MyPageAchievementResponse.builder()
                            .id(achievement.getId())
                            .name(achievement.getName())
                            .description(achievement.getDescription())
                            .conditionType(achievement.getConditionType())
                            .conditionValue(achievement.getConditionValue())
                            .isAcquired(ma != null)
                            .acquiredAt(ma != null ? ma.getAcquiredAt() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 방탈출 결과 기록 후 기본 업적(TOTAL_PLAY_COUNT, CLEAR_TIME_UNDER)을 자동 지급한다.
     *
     * @param member        업적 대상 회원
     * @param completedCount COMPLETED 상태 예약 수
     * @param isCleared     이번 결과의 클리어 여부
     * @param clearTime     이번 결과의 클리어 시간 (null 가능)
     */
    @Transactional
    public void grantResultAchievements(Member member, long completedCount, Boolean isCleared, LocalTime clearTime) {
        // 이미 획득한 업적 ID Set 구성 (중복 지급 방지)
        Set<Long> acquiredIds = memberAchievementRepository.findByMember_Id(member.getId())
                .stream()
                .map(ma -> ma.getAchievement().getId())
                .collect(Collectors.toSet());

        LocalDateTime now = LocalDateTime.now();

        // TOTAL_PLAY_COUNT 업적 체크 — completedCount >= conditionValue
        achievementRepository.findByConditionType(AchievementConditionType.TOTAL_PLAY_COUNT)
                .stream()
                .filter(a -> !acquiredIds.contains(a.getId()))
                .filter(a -> completedCount >= a.getConditionValue())
                .forEach(a -> memberAchievementRepository.save(
                        MemberAchievement.builder()
                                .member(member)
                                .achievement(a)
                                .acquiredAt(now)
                                .build()
                ));

        // CLEAR_TIME_UNDER 업적 체크 — isCleared=true 이고 clearTime이 있을 때만
        if (Boolean.TRUE.equals(isCleared) && clearTime != null) {
            int clearMinutes = clearTime.getHour() * 60 + clearTime.getMinute();

            achievementRepository.findByConditionType(AchievementConditionType.CLEAR_TIME_UNDER)
                    .stream()
                    .filter(a -> !acquiredIds.contains(a.getId()))
                    .filter(a -> clearMinutes <= a.getConditionValue())
                    .forEach(a -> memberAchievementRepository.save(
                            MemberAchievement.builder()
                                    .member(member)
                                    .achievement(a)
                                    .acquiredAt(now)
                                    .build()
                    ));
        }
    }

    /**
     * 미니게임 클리어 시 업적 지급 여부를 검사하고 지급한다.
     */
    @Transactional
    public AchievementGrantResult grantMinigameClearAchievement(Member member) {
        Set<Long> acquiredIds = memberAchievementRepository.findByMember_Id(member.getId())
                .stream()
                .map(ma -> ma.getAchievement().getId())
                .collect(Collectors.toSet());

        List<Achievement> minigameAchievements = achievementRepository.findByConditionType(AchievementConditionType.MINIGAME_CLEAR);

        if (minigameAchievements.isEmpty()) {
            return new AchievementGrantResult(false, null);
        }

        // 현재 미니게임 클리어 업적은 1개만 사용
        Achievement targetAchievement = minigameAchievements.get(0);

        if (acquiredIds.contains(targetAchievement.getId())) {
            return new AchievementGrantResult(false, targetAchievement);
        }

        LocalDateTime now = LocalDateTime.now();
        memberAchievementRepository.save(
                MemberAchievement.builder()
                        .member(member)
                        .achievement(targetAchievement)
                        .acquiredAt(now)
                        .build()
        );

        return new AchievementGrantResult(true, targetAchievement);
    }
}
