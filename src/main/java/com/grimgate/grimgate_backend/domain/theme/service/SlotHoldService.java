package com.grimgate.grimgate_backend.domain.theme.service;

import com.grimgate.grimgate_backend.domain.theme.dto.SlotHoldRequest;
import com.grimgate.grimgate_backend.domain.theme.dto.SlotHoldResponse;
import com.grimgate.grimgate_backend.domain.theme.entity.TimeSlot;
import com.grimgate.grimgate_backend.domain.theme.entity.TimeSlotStatus;
import com.grimgate.grimgate_backend.domain.theme.repository.TimeSlotRepository;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 타임슬롯에 대한 Redis 임시 선점(HOLD) 비즈니스 로직을 처리하는 Service 클래스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SlotHoldService {

    private final TimeSlotRepository timeSlotRepository;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 특정 타임슬롯을 5분 동안 임시 선점(HOLD)합니다.
     *
     * @param timeSlotId 임시 선점할 타임슬롯 ID
     * @param request    요청자 회원 정보가 포함된 DTO
     * @return 임시 선점 결과 응답 DTO
     */
    @Transactional
    public SlotHoldResponse holdSlot(Long timeSlotId, SlotHoldRequest request) {
        // 1. timeSlotId로 TimeSlot 조회
        TimeSlot timeSlot = timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 슬롯입니다."));

        // 2. timeSlot.status가 SLOT_AVAILABLE이 아니면 409 Conflict
        if (timeSlot.getStatus() != TimeSlotStatus.SLOT_AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "예약 가능한 슬롯 상태가 아닙니다.");
        }

        // 3. UUID 기반 holdToken 생성
        String holdToken = UUID.randomUUID().toString();
        String key = "hold:slot:" + timeSlotId;
        String value = request.getMemberId() + ":" + holdToken;

        // 4. Redis에 setIfAbsent(key, value, Duration.ofMinutes(5))로 원자적으로 임시 선점 처리
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, value, Duration.ofMinutes(5));

        // 5. setIfAbsent 결과가 false 또는 null이면 이미 HOLD 중인 슬롯으로 판단하여 409 Conflict
        if (success == null || !success) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 다른 사용자가 선점 중인 슬롯입니다.");
        }

        // 6. 성공 시 응답 DTO 빌드 및 반환
        return SlotHoldResponse.builder()
                .timeSlotId(timeSlotId)
                .holdToken(holdToken)
                .expiresInSeconds(300L)
                .build();
    }
}
