package com.grimgate.grimgate_backend.domain.theme.controller;

import com.grimgate.grimgate_backend.domain.theme.dto.BranchDetailResponse;
import com.grimgate.grimgate_backend.domain.theme.dto.ThemeDetailResponse;
import com.grimgate.grimgate_backend.domain.theme.dto.ThemeResponse;
import com.grimgate.grimgate_backend.domain.theme.dto.ThemeSearchCondition;
import com.grimgate.grimgate_backend.domain.theme.entity.Theme;
import com.grimgate.grimgate_backend.domain.theme.repository.ThemeRepository;
import com.grimgate.grimgate_backend.domain.theme.service.ThemeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.grimgate.grimgate_backend.domain.theme.dto.TimeSlotResponse;
import com.grimgate.grimgate_backend.domain.theme.service.ThemeService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/themes")
public class ThemeController {

    private final ThemeService themeService;

    @GetMapping("")
    public List<ThemeResponse> getThemes(
            ThemeSearchCondition condition
    ) {
        return themeService.getThemes(condition);
    }

    @GetMapping("/{id}")
    public ThemeDetailResponse getThemeDetail(@PathVariable Long id) {
        return themeService.getThemeDetail(id);
    }

    @GetMapping("/branches/{id}")
    public BranchDetailResponse getBranches(@PathVariable Long id) {
        return themeService.getBranches(id);
    }


    /**
     * 특정 테마의 특정 날짜에 해당하는 타임슬롯 목록을 조회합니다.
     * API 경로: GET /api/themes/{id}/slots
     *
     * @param id       테마 식별자 (ID)
     * @param slotDate 조회할 날짜 (Query Parameter: slot_date, ISO형식 yyyy-MM-dd)
     * @return 타임슬롯 응답 DTO 목록이 담긴 ResponseEntity
     */
    @GetMapping("/{id}/slots")
    public ResponseEntity<List<TimeSlotResponse>> getThemeSlots(
            @PathVariable("id") Long id,
            @RequestParam("slot_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate slotDate
    ) {
        List<TimeSlotResponse> responses = themeService.getSlotsByThemeAndDate(id, slotDate);
        return ResponseEntity.ok(responses);
    }
}
