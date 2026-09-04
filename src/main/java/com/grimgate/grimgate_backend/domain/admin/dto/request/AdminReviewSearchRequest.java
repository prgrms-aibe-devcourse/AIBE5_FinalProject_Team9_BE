package com.grimgate.grimgate_backend.domain.admin.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

// 관리자 후기 목록 검색 조건 DTO (@ModelAttribute 바인딩용)
@Getter
@Setter
@NoArgsConstructor
public class AdminReviewSearchRequest {

    // 후기 상태 필터 (ACTIVE / HIDDEN / null이면 전체)
    private String status;

    // 특정 테마 후기만 조회할 때 사용하는 테마 ID
    private Long themeId;

    // 작성일 시작일 (해당 날짜 00:00:00 이상)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;

    // 작성일 종료일 (해당 날짜 23:59:59 이하)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;

    // 후기 본문 또는 작성자 닉네임 검색어
    private String keyword;

    // 페이지 번호 (0부터 시작)
    private int page = 0;

    // 페이지당 조회 건수
    private int limit = 16;
}
