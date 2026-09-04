package com.grimgate.grimgate_backend.domain.theme.controller;

import com.grimgate.grimgate_backend.domain.theme.dto.SlotHoldResponse;
import com.grimgate.grimgate_backend.domain.theme.dto.SlotReleaseRequest;
import com.grimgate.grimgate_backend.domain.theme.dto.SlotReleaseResponse;
import com.grimgate.grimgate_backend.domain.theme.service.SlotHoldService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
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
     * @return 임시 선점 결과 DTO
     */
    @PostMapping("/{id}/hold")
    public ResponseEntity<SlotHoldResponse> holdSlot(
            @PathVariable("id") Long timeSlotId
    ) {
        SlotHoldResponse response = slotHoldService.holdSlot(timeSlotId);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 타임슬롯의 임시 선점(HOLD) 상태를 해제합니다.
     *
     * @param timeSlotId 임시 선점 해제할 타임슬롯 ID
     * @param request    선점 해제 요청 정보 DTO
     * @return 임시 선점 해제 결과 DTO
     */
    @PatchMapping("/{id}/release")
    public ResponseEntity<SlotReleaseResponse> releaseSlot(
            @PathVariable("id") Long timeSlotId,
            @Valid @RequestBody SlotReleaseRequest request
    ) {
        SlotReleaseResponse response = slotHoldService.releaseSlot(timeSlotId, request);
        return ResponseEntity.ok(response);
    }
}
