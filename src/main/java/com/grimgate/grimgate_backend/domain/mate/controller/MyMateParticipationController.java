package com.grimgate.grimgate_backend.domain.mate.controller;

import com.grimgate.grimgate_backend.domain.mate.dto.MateParticipantResponse;
import com.grimgate.grimgate_backend.domain.mate.service.MateParticipantService;
import com.grimgate.grimgate_backend.global.security.SecurityUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내가 참여한 메이트 목록 조회 API.
 *
 * <p>기능명세 MB-009 / MY-010: {@code GET /api/members/me/mate-participations}.</p>
 *
 * <p>경로상 Member 도메인 책임이지만 응답은 메이트 참여 정보이므로 mate 패키지에 둔다.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members/me")
public class MyMateParticipationController {

    private final MateParticipantService mateParticipantService;

    /** 내가 참여한 메이트 목록 (활성 참여 + 모집글 살아있는 것만) */
    @GetMapping("/mate-participations")
    public ResponseEntity<List<MateParticipantResponse>> myParticipations() {
        Long accountId = SecurityUtil.getCurrentAccountId();
        return ResponseEntity.ok(mateParticipantService.myParticipations(accountId));
    }
}
