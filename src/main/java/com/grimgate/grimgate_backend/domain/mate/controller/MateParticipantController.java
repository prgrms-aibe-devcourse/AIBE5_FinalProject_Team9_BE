package com.grimgate.grimgate_backend.domain.mate.controller;

import com.grimgate.grimgate_backend.domain.mate.dto.MateParticipantListResponse;
import com.grimgate.grimgate_backend.domain.mate.dto.MateParticipantResponse;
import com.grimgate.grimgate_backend.domain.mate.service.MateParticipantService;
import com.grimgate.grimgate_backend.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 메이트 모집글 참여 컨트롤러.
 *
 * <p>base path: {@code /api/mate-posts/{postId}}</p>
 *
 * <p>기능명세 MP-001 ~ MP-003 준수</p>
 * <ul>
 *   <li>POST   /api/mate-posts/{postId}/join                        — 참가 신청 (로그인 필수)</li>
 *   <li>DELETE /api/mate-posts/{postId}/join                        — 본인 참가 취소 (로그인 필수)</li>
 *   <li>DELETE /api/mate-posts/{postId}/participants/{memberId}     — 강퇴 (작성자만, 내부용 확장)</li>
 *   <li>GET    /api/mate-posts/{postId}/participants                — 참여자 목록 (작성자만 조회 가능)</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mate-posts")
public class MateParticipantController {

    private final MateParticipantService mateParticipantService;

    /** 참가 신청 (MP-001) - 201 + openChatUrl 포함 응답 */
    @PostMapping("/{postId}/join")
    public ResponseEntity<MateParticipantResponse> join(@PathVariable("postId") Long postId) {
        Long accountId = SecurityUtil.getCurrentAccountId();
        MateParticipantResponse res = mateParticipantService.join(accountId, postId);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    /** 본인 참가 취소 (MP-002) - 200 + 변경 정보 응답 */
    @DeleteMapping("/{postId}/join")
    public ResponseEntity<MateParticipantResponse> cancel(@PathVariable("postId") Long postId) {
        Long accountId = SecurityUtil.getCurrentAccountId();
        return ResponseEntity.ok(mateParticipantService.cancel(accountId, postId));
    }

    /** 작성자가 참여자 강퇴 (내부 확장 기능) */
    @DeleteMapping("/{postId}/participants/{memberId}")
    public ResponseEntity<Void> kick(@PathVariable("postId") Long postId,
                                     @PathVariable("memberId") Long memberId) {
        Long accountId = SecurityUtil.getCurrentAccountId();
        mateParticipantService.kick(accountId, postId, memberId);
        return ResponseEntity.noContent().build();
    }

    /** 참여자 목록 조회 (MP-003) - 작성자만 */
    @GetMapping("/{postId}/participants")
    public ResponseEntity<MateParticipantListResponse> list(@PathVariable("postId") Long postId) {
        Long accountId = SecurityUtil.getCurrentAccountId();
        return ResponseEntity.ok(mateParticipantService.listParticipants(accountId, postId));
    }
}
