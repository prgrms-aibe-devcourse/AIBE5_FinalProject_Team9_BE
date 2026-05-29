package com.grimgate.grimgate_backend.domain.theme.controller;

import com.grimgate.grimgate_backend.domain.theme.dto.SlotHoldRequest;
import com.grimgate.grimgate_backend.domain.theme.dto.SlotHoldResponse;
import com.grimgate.grimgate_backend.domain.theme.service.SlotHoldService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 슬롯 임시 선점(HOLD) API를 제공하는 Controller 클래스입니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/slots")
public class SlotHoldController {

    private final SlotHoldService slotHoldService;

    /**
     * 특정 타임슬롯을 임시 선점(HOLD)합니다.
     *
     * @param timeSlotId 임시 선점할 타임슬롯 ID
     * @param request    요청자 회원 정보 DTO
     * @return 임시 선점 결과 DTO
     */
    @PostMapping("/{timeSlotId}/hold")
    public ResponseEntity<SlotHoldResponse> holdSlot(
            @PathVariable("timeSlotId") Long timeSlotId,
            @Valid @RequestBody SlotHoldRequest request
    ) {
        SlotHoldResponse response = slotHoldService.holdSlot(timeSlotId, request);
        return ResponseEntity.ok(response);
    }
}
