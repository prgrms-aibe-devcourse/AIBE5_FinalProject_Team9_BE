package com.grimgate.grimgate_backend.domain.mate.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 메이트 모집글 참여자 목록 응답 DTO.
 *
 * <p>모집글 상세 페이지의 "참여자" 섹션에 사용한다.</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MateParticipantListResponse {

    private Long matePostId;
    private Integer currentPeople;
    private Integer maxPeople;
    private List<MateParticipantResponse> items;
}
