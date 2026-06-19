package com.grimgate.grimgate_backend.domain.mate.controller;

import com.grimgate.grimgate_backend.domain.mate.dto.MateCommentCreateRequest;
import com.grimgate.grimgate_backend.domain.mate.dto.MateCommentListResponse;
import com.grimgate.grimgate_backend.domain.mate.dto.MateCommentResponse;
import com.grimgate.grimgate_backend.domain.mate.dto.MateCommentUpdateRequest;
import com.grimgate.grimgate_backend.domain.mate.dto.MateReplyResponse;
import com.grimgate.grimgate_backend.domain.mate.service.MateCommentService;
import com.grimgate.grimgate_backend.global.security.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 메이트 모집글 댓글 컨트롤러.
 *
 * <p>base path: {@code /api/mate-posts/{postId}/comments}</p>
 *
 * <p>인증: 조회는 비로그인 허용, 작성/수정/삭제는 {@link SecurityUtil#getCurrentAccountId()} 로 인증 필수.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mate-posts/{postId}/comments")
public class MateCommentController {

    private final MateCommentService mateCommentService;

    /** 댓글 목록 조회 — 비로그인 허용 */
    @GetMapping
    public ResponseEntity<MateCommentListResponse> list(@PathVariable Long postId) {
        return ResponseEntity.ok(mateCommentService.list(postId));
    }

    /** 댓글 작성 — 로그인 필수 */
    @PostMapping
    public ResponseEntity<MateCommentResponse> create(
            @PathVariable Long postId,
            @Valid @RequestBody MateCommentCreateRequest request
    ) {
        Long accountId = SecurityUtil.getCurrentAccountId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mateCommentService.create(accountId, postId, request));
    }

    /** 댓글 수정 — 댓글 작성자 본인만 가능 */
    @PatchMapping("/{commentId}")
    public ResponseEntity<MateCommentResponse> update(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody MateCommentUpdateRequest request
    ) {
        Long accountId = SecurityUtil.getCurrentAccountId();
        return ResponseEntity.ok(mateCommentService.update(accountId, postId, commentId, request));
    }

    /** 댓글 삭제 (soft delete) — 댓글 작성자 본인만 가능 */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        Long accountId = SecurityUtil.getCurrentAccountId();
        mateCommentService.delete(accountId, postId, commentId);
        return ResponseEntity.noContent().build();
    }

    /** 대댓글 작성 — 로그인 필수, 1-depth만 허용 */
    @PostMapping("/{commentId}/replies")
    public ResponseEntity<MateReplyResponse> createReply(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody MateCommentCreateRequest request
    ) {
        Long accountId = SecurityUtil.getCurrentAccountId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mateCommentService.createReply(accountId, postId, commentId, request));
    }
}
