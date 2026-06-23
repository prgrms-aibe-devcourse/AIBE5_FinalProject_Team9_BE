package com.grimgate.grimgate_backend.domain.minigame.service;

import com.grimgate.grimgate_backend.domain.achievement.service.AchievementService;
import com.grimgate.grimgate_backend.domain.achievement.dto.AchievementGrantResult;
import com.grimgate.grimgate_backend.domain.member.entity.Member;
import com.grimgate.grimgate_backend.domain.member.repository.MemberRepository;
import com.grimgate.grimgate_backend.domain.minigame.dto.MinigameClearResponse;
import com.grimgate.grimgate_backend.global.exception.CustomException;
import com.grimgate.grimgate_backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MinigameService {

    private final MemberRepository memberRepository;
    private final AchievementService achievementService;

    @Transactional
    public MinigameClearResponse clearMinigame(Long accountId) {
        Member member = memberRepository.findByAccount_Id(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        AchievementGrantResult result = achievementService.grantMinigameClearAchievement(member);

        return MinigameClearResponse.builder()
                .newAcquired(result.newAcquired())
                .achievement(MinigameClearResponse.AchievementInfo.from(result.achievement()))
                .build();
    }
}
