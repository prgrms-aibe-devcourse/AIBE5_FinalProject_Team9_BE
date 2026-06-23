package com.grimgate.grimgate_backend.domain.minigame.dto;

import com.grimgate.grimgate_backend.domain.achievement.entity.Achievement;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "미니게임 클리어 응답")
public class MinigameClearResponse {

    @Schema(description = "업적 신규 획득 여부", example = "true")
    private boolean newAcquired;

    @Schema(description = "획득한 미니게임 업적 정보")
    private AchievementInfo achievement;

    @Getter
    @Builder
    @Schema(description = "업적 정보")
    public static class AchievementInfo {
        @Schema(description = "업적 ID", example = "7")
        private Long id;

        @Schema(description = "업적 이름", example = "미니게임의 신")
        private String name;

        @Schema(description = "업적 설명", example = "미니게임을 클리어했습니다.")
        private String description;

        public static AchievementInfo from(Achievement achievement) {
            if (achievement == null) return null;
            return AchievementInfo.builder()
                    .id(achievement.getId())
                    .name(achievement.getName())
                    .description(achievement.getDescription())
                    .build();
        }
    }
}
