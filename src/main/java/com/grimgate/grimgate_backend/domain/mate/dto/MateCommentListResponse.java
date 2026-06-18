package com.grimgate.grimgate_backend.domain.mate.dto;

import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 메이트 모집글 댓글 목록 응답 DTO */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MateCommentListResponse {

    private int totalCount;
    private List<MateCommentResponse> comments;

    public static MateCommentListResponse of(List<MateCommentResponse> comments) {
        return MateCommentListResponse.builder()
                .totalCount(comments.size())
                .comments(comments)
                .build();
    }
}
