package com.grimgate.grimgate_backend.domain.achievement.entity;

/**
 * 업적 달성 조건 타입
 */
public enum AchievementConditionType {

    // 전체 플레이 횟수 (성공+실패)
    TOTAL_PLAY_COUNT,

    // n분 이내 클리어 (clear_time <= conditionValue)
    CLEAR_TIME_UNDER,

    // horror_level 5 성공 횟수
    HORROR_LEVEL_SUCCESS,

    // 메이트 참여/모집 횟수
    MATE_PARTICIPATE_COUNT,

    // 같은 메이트와 함께한 횟수 (mate_participant 기준)
    SAME_MATE_COUNT,

    // 미니게임 클리어
    MINIGAME_CLEAR
}
