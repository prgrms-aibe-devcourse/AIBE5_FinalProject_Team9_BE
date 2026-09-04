package com.grimgate.grimgate_backend.domain.review.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 후기 신고 요청 DTO
@Getter
@NoArgsConstructor
public class ReviewReportCreateRequest {

    @NotBlank
    private String reason;

    private String detail;
}
