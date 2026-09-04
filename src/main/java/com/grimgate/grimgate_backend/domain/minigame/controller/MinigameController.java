package com.grimgate.grimgate_backend.domain.minigame.controller;

import com.grimgate.grimgate_backend.domain.minigame.dto.MinigameClearResponse;
import com.grimgate.grimgate_backend.domain.minigame.service.MinigameService;
import com.grimgate.grimgate_backend.global.response.ApiResponse;
import com.grimgate.grimgate_backend.global.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Minigame", description = "미니게임 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/minigames")
public class MinigameController {

    private final MinigameService minigameService;

    @PostMapping("/clear")
    @Operation(summary = "미니게임 클리어 처리 및 업적 지급", description = "미니게임을 클리어했을 때 호출하여 업적을 자동 지급받습니다.")
    public ResponseEntity<ApiResponse<MinigameClearResponse>> clearMinigame() {
        Long accountId = SecurityUtil.getCurrentAccountId();
        MinigameClearResponse response = minigameService.clearMinigame(accountId);
        return ResponseEntity.ok(ApiResponse.success("미니게임 클리어 처리 성공", response));
    }
}
