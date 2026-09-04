package com.grimgate.grimgate_backend.domain.mate.dto;

import com.grimgate.grimgate_backend.domain.mate.entity.ExperienceLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 메이트 모집글 수정 요청 DTO.
 * — 부분 수정(PATCH). null 필드는 무시.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MatePostUpdateRequest {

    @Size(min = 5, max = 50, message = "제목은 5~50자여야 합니다.")
    private String title;

    @Size(max = 1000, message = "내용은 최대 1000자입니다.")
    private String content;

    private LocalDateTime meetingTime;

    private LocalDateTime deadline;

    @Min(value = 2, message = "최대 인원은 2명 이상이어야 합니다.")
    @Max(value = 6, message = "최대 인원은 6명 이하여야 합니다.")
    private Integer maxPeople;

    private List<String> tags;

    private ExperienceLevel experienceLevel;

    @Pattern(regexp = "^https?://open\\.kakao\\.com/.+",
            message = "카카오 오픈채팅 URL 형식이 올바르지 않습니다.")
    private String openChatUrl;

    private String imageUrl;
}
