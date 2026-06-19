package com.grimgate.grimgate_backend.domain.mate.dto;

import com.grimgate.grimgate_backend.domain.mate.entity.MateComment;
import com.grimgate.grimgate_backend.domain.member.entity.Member;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 메이트 모집글 대댓글 단건 응답 DTO (순환참조 방지를 위해 replies 필드 없음) */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MateReplyResponse {

    private Long commentId;
    private Long parentCommentId;
    private Long authorId;
    private String authorNickname;
    private String content;
    private boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MateReplyResponse of(MateComment reply) {
        Member member = reply.getMember();
        Long parentId = reply.getParent() != null ? reply.getParent().getId() : null;
        return MateReplyResponse.builder()
                .commentId(reply.getId())
                .parentCommentId(parentId)
                .authorId(member != null ? member.getId() : null)
                .authorNickname(resolveNickname(member))
                .content(reply.getContent())
                .isDeleted(reply.isDeleted())
                .createdAt(reply.getCreatedAt())
                .updatedAt(reply.getUpdatedAt())
                .build();
    }

    /** Account.nickname을 작성자 닉네임으로 사용. 탈퇴 회원(account == null)은 "탈퇴한 회원" 반환 */
    private static String resolveNickname(Member member) {
        if (member == null || member.getAccount() == null) return "탈퇴한 회원";
        return member.getAccount().getNickname();
    }
}
