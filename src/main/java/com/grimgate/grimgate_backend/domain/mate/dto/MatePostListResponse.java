package com.grimgate.grimgate_backend.domain.mate.dto;

import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

/**
 * 메이트 모집글 목록 응답 (페이지네이션 포함).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MatePostListResponse {

    private List<MatePostResponse> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;

    public static MatePostListResponse from(Page<MatePostResponse> page) {
        return MatePostListResponse.builder()
                .items(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }
}
