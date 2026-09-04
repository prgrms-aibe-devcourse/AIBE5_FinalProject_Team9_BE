package com.grimgate.grimgate_backend.domain.mate.dto;

import com.grimgate.grimgate_backend.domain.mate.entity.ExperienceLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
 * 메이트 모집글 생성 요청 DTO.
 * — API 명세 3-1 기준
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MatePostCreateRequest {

    @NotNull(message = "테마 ID는 필수입니다.")
    private Long themeId;

    @NotBlank(message = "제목은 필수입니다.")
    @Size(min = 5, max = 50, message = "제목은 5~50자여야 합니다.")
    private String title;

    @Size(max = 1000, message = "내용은 최대 1000자입니다.")
    private String content;

    @NotNull(message = "모임 시간은 필수입니다.")
    private LocalDateTime meetingTime;

    /** 모집 마감일 (선택). meetingTime 이전이어야 함. */
    private LocalDateTime deadline;

    @NotNull(message = "최대 인원은 필수입니다.")
    @Min(value = 2, message = "최대 인원은 2명 이상이어야 합니다.")
    @Max(value = 6, message = "최대 인원은 6명 이하여야 합니다.")
    private Integer maxPeople;

    /** 분위기 태그 — 최대 5개, 서비스에서 콤마로 직렬화하여 저장 */
    private List<String> tags;

    private ExperienceLevel experienceLevel;

    @NotBlank(message = "카카오 오픈채팅 URL은 필수입니다.")
    @Pattern(regexp = "^https?://open\\.kakao\\.com/.+",
            message = "카카오 오픈채팅 URL 형식이 올바르지 않습니다.")
    private String openChatUrl;

    private String imageUrl;
}
