package com.grimgate.grimgate_backend.domain.owner.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReservationResultRequest {

    @NotNull(message = "클리어 여부는 필수입니다.")
    private Boolean isCleared;

    private LocalTime clearTime;
}
