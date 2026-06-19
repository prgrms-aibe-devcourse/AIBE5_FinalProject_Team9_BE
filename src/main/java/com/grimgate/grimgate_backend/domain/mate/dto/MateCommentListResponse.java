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

    /** 삭제되지 않은 원댓글 + 대댓글 전체 개수 */
    private int totalCount;
    private List<MateCommentResponse> comments;

    /**
     * @param comments   원댓글 + replies 조립이 완료된 목록
     * @param totalCount 삭제되지 않은 원댓글 + 대댓글 전체 합산
     */
    public static MateCommentListResponse of(List<MateCommentResponse> comments, int totalCount) {
        return MateCommentListResponse.builder()
                .totalCount(totalCount)
                .comments(comments)
                .build();
    }
}
