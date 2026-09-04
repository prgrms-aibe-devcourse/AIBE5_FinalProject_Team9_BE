package com.grimgate.grimgate_backend.domain.mate.dto;

import com.grimgate.grimgate_backend.domain.mate.entity.MateComment;
import com.grimgate.grimgate_backend.domain.member.entity.Member;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 메이트 모집글 원댓글 단건 응답 DTO */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MateCommentResponse {

    private Long commentId;
    /** 원댓글은 항상 null */
    private Long parentCommentId;
    private Long authorId;
    private String authorNickname;
    private String content;
    private boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** 이 원댓글에 달린 대댓글 목록 (삭제된 대댓글 제외) */
    private List<MateReplyResponse> replies;

    /**
     * 목록 조회용 — replies 포함, 삭제된 원댓글 마스킹 적용.
     * 삭제된 원댓글은 content와 authorNickname을 마스킹하여 반환한다.
     */
    public static MateCommentResponse of(MateComment comment, List<MateReplyResponse> replies) {
        Member member = comment.getMember();
        boolean deleted = comment.isDeleted();
        return MateCommentResponse.builder()
                .commentId(comment.getId())
                .parentCommentId(null)
                .authorId(deleted ? null : (member != null ? member.getId() : null))
                .authorNickname(deleted ? "삭제됨" : resolveNickname(member))
                .content(deleted ? "삭제된 댓글입니다." : comment.getContent())
                .isDeleted(deleted)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .replies(replies)
                .build();
    }

    /**
     * 단건 작성/수정 응답용 — replies 없이 빈 리스트로 반환.
     */
    public static MateCommentResponse of(MateComment comment) {
        return of(comment, List.of());
    }

    /** Account.nickname 을 작성자 닉네임으로 사용. 탈퇴 회원(account == null)은 "탈퇴한 회원" 반환 */
    private static String resolveNickname(Member member) {
        if (member == null || member.getAccount() == null) return "탈퇴한 회원";
        return member.getAccount().getNickname();
    }
}
