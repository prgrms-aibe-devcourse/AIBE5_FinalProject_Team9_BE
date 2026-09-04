package com.grimgate.grimgate_backend.domain.owner.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사장님의 후기 신고 숨김 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReportHideRequest {

    @NotBlank(message = "숨김 사유는 필수입니다.")
    private String ownerReason;
}
