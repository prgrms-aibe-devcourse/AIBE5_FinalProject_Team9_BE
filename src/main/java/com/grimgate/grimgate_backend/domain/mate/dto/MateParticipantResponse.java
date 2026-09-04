package com.grimgate.grimgate_backend.domain.mate.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.grimgate.grimgate_backend.domain.mate.entity.MateParticipant;
import com.grimgate.grimgate_backend.domain.mate.entity.MateParticipantStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 메이트 모집글 참여자 단건 응답 DTO.
 *
 * <p>응답 키는 카멜케이스, 엔티티 컬럼은 스네이크케이스라는 컨벤션을 따른다.</p>
 *
 * <p>{@link #openChatUrl}은 참가 신청(MP-001) 성공 응답에만 포함되고, 일반 목록/취소 응답에서는
 * null 이므로 직렬화 시 제외된다.</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MateParticipantResponse {

    private Long id;
    private Long matePostId;
    private Long memberId;
    private String memberNickname;
    private MateParticipantStatus status;
    private LocalDateTime joinedAt;
    private LocalDateTime cancelledAt;

    /** 참가 성공 시에만 노출되는 오픈채팅 URL (MP-001 비고) */
    private String openChatUrl;

    /** 일반 목록/취소 응답용 - openChatUrl 미포함 */
    public static MateParticipantResponse from(MateParticipant p) {
        return MateParticipantResponse.builder()
                .id(p.getId())
                .matePostId(p.getMatePost().getId())
                .memberId(p.getMember().getId())
                .memberNickname(p.getMember().getAccount().getNickname())
                .status(p.getStatus())
                .joinedAt(p.getJoinedAt())
                .cancelledAt(p.getCancelledAt())
                .build();
    }

    /** 참가 신청 성공 응답용 - openChatUrl 포함 */
    public static MateParticipantResponse fromWithOpenChat(MateParticipant p) {
        return MateParticipantResponse.builder()
                .id(p.getId())
                .matePostId(p.getMatePost().getId())
                .memberId(p.getMember().getId())
                .memberNickname(p.getMember().getAccount().getNickname())
                .status(p.getStatus())
                .joinedAt(p.getJoinedAt())
                .cancelledAt(p.getCancelledAt())
                .openChatUrl(p.getMatePost().getOpenChatUrl())
                .build();
    }
}
