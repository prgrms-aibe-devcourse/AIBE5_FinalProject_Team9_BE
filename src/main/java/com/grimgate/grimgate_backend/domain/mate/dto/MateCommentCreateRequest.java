package com.grimgate.grimgate_backend.domain.mate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 메이트 모집글 댓글 작성 요청 DTO */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MateCommentCreateRequest {

    @NotBlank(message = "댓글 내용을 입력해주세요.")
    @Size(min = 1, max = 500, message = "댓글은 1자 이상 500자 이하로 입력해주세요.")
    private String content;
}
