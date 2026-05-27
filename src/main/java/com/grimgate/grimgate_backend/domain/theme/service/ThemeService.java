package com.grimgate.grimgate_backend.domain.theme.service;

import com.grimgate.grimgate_backend.domain.theme.dto.TimeSlotResponse;
import com.grimgate.grimgate_backend.domain.theme.entity.TimeSlot;
import com.grimgate.grimgate_backend.domain.theme.repository.ThemeRepository;
import com.grimgate.grimgate_backend.domain.theme.repository.TimeSlotRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 방탈출 테마 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 
 * [Service 계층에서 테마 검증과 슬롯 조회를 처리하는 이유]
 * - Controller는 클라이언트와의 인터페이스(요청 매핑, 단순 파라미터 타입 변환 등)에 집중해야 합니다.
 * - 비즈니스 정합성(예: 테마의 실존 여부 검증) 및 비즈니스 예외 처리는 트랜잭션 경계 안에서 안전하게 보호되어야 하므로 Service 계층에서 담당합니다.
 * - 데이터 조회 로직과 검증 로직이 결합된 핵심 워크플로우를 Service 계층에 둠으로써, 향후 다른 API나 내부 로직에서 
 *   동일한 검증 프로세스를 재사용할 수 있으며, 데이터베이스 계층의 영속성 컨텍스트(Lazy Loading 등)를 안정적으로 활용할 수 있습니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThemeService {

    private final ThemeRepository themeRepository;
    private final TimeSlotRepository timeSlotRepository;

    /**
     * 특정 테마의 특정 날짜에 해당하는 예약 가능 타임슬롯 목록을 조회합니다.
     *
     * @param themeId  조회할 테마의 식별자
     * @param slotDate 조회할 날짜
     * @return 타임슬롯 응답 DTO 목록 (슬롯 데이터가 없는 경우 빈 리스트 반환)
     * @throws ResponseStatusException 테마 ID에 해당하는 테마가 존재하지 않을 때 404 예외 발생
     */
    public List<TimeSlotResponse> getSlotsByThemeAndDate(Long themeId, LocalDate slotDate) {
        // 1. 테마 존재 여부 검증
        // 도메인 데이터의 정합성을 확인하기 위해 DB에 해당 테마가 존재하는지 검증하며, 없는 경우 404 Not Found를 응답하도록 예외를 던집니다.
        themeRepository.findById(themeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 테마입니다. ID: " + themeId));

        // 2. 특정 날짜의 타임슬롯 조회
        // 특정 날짜에 등록된 슬롯이 없는 경우 예외가 아닌 빈 리스트를 반환하여, 사용자에게 해당 날짜에 예약할 수 있는 타임라인 자체가 비어있음을 보여줍니다.
        List<TimeSlot> timeSlots =
        timeSlotRepository.findByThemeIdAndSlotDateOrderByStartTimeAsc(themeId, slotDate);

        // 3. DTO 변환
        return timeSlots.stream()
                .map(TimeSlotResponse::from)
                .toList();
    }
}
