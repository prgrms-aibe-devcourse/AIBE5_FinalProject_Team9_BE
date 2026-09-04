package com.grimgate.grimgate_backend.domain.owner.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class OwnerReservationStatsResponse {

    private Long totalCount;
    private Long todayCount;
    private Long completedCount;
    private Long confirmedCount;
    private Long cancelledCount;
}
