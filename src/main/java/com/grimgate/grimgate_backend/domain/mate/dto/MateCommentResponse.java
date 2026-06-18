package com.grimgate.grimgate_backend.domain.mate.dto;

import com.grimgate.grimgate_backend.domain.mate.entity.MateComment;
import com.grimgate.grimgate_backend.domain.member.entity.Member;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 메이트 모집글 댓글 단건 응답 DTO */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MateCommentResponse {

    private Long commentId;
    private Long authorId;
    private String authorNickname;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MateCommentResponse of(MateComment comment) {
        Member member = comment.getMember();
        return MateCommentResponse.builder()
                .commentId(comment.getId())
                .authorId(member != null ? member.getId() : null)
                .authorNickname(resolveNickname(member))
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    /** Account.nickname 을 작성자 닉네임으로 사용 */
    private static String resolveNickname(Member member) {
        if (member == null || member.getAccount() == null) return null;
        return member.getAccount().getNickname();
    }
}
