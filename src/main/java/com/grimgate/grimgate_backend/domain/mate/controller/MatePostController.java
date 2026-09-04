package com.grimgate.grimgate_backend.domain.mate.controller;

import com.grimgate.grimgate_backend.domain.mate.dto.MatePostCreateRequest;
import com.grimgate.grimgate_backend.domain.mate.dto.MatePostListResponse;
import com.grimgate.grimgate_backend.domain.mate.dto.MatePostResponse;
import com.grimgate.grimgate_backend.domain.mate.dto.MatePostStatsResponse;
import com.grimgate.grimgate_backend.domain.mate.dto.MatePostUpdateRequest;
import com.grimgate.grimgate_backend.domain.mate.service.MatePostService;
import com.grimgate.grimgate_backend.global.security.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 메이트 모집글 컨트롤러.
 *
 * <p>API 명세(3_API 명세) 기준 base path: {@code /api/mate-posts}</p>
 *
 * <p>인증 사용자 식별은 {@link SecurityUtil#getCurrentAccountId()} 로 통일.
 * 비로그인이 허용되는 조회 API(list, detail)는 SecurityContext 가 비어있어도 정상 동작하도록 처리한다.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mate-posts")
public class MatePostController {

    private final MatePostService matePostService;

    /* ===== Post CRUD ===== */

    /** 3-1 모집글 목록 조회 — 비로그인 허용, 로그인 시 '내글' 탭 사용 가능 */
    @GetMapping
    public ResponseEntity<MatePostListResponse> list(
            @RequestParam(value = "tab", required = false, defaultValue = "all") String tab,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "themeId", required = false) Long themeId,
            @RequestParam(value = "experienceLevel", required = false) String experienceLevel,
            @RequestParam(value = "sort", required = false, defaultValue = "latest") String sort,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size
    ) {
        Long currentAccountId = getOptionalAccountId();
        return ResponseEntity.ok(matePostService.list(
                tab, keyword, status, themeId, experienceLevel, sort, page, size, currentAccountId));
    }

    /** 3-7 통계 — list 보다 위에 매핑 (path 충돌 방지) */
    @GetMapping("/stats")
    public ResponseEntity<MatePostStatsResponse> stats() {
        return ResponseEntity.ok(matePostService.stats());
    }

    /** 3-2 모집글 생성 — 로그인 필수 */
    @PostMapping
    public ResponseEntity<MatePostResponse> create(@Valid @RequestBody MatePostCreateRequest request) {
        Long accountId = SecurityUtil.getCurrentAccountId();
        MatePostResponse res = matePostService.create(accountId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    /** 3-3 모집글 상세 — 비로그인 허용, 작성자에게는 openChatUrl 노출 */
    @GetMapping("/{id}")
    public ResponseEntity<MatePostResponse> detail(@PathVariable("id") Long id) {
        Long currentAccountId = getOptionalAccountId();
        return ResponseEntity.ok(matePostService.getDetail(id, currentAccountId));
    }

    /** 3-4 모집글 수정 (작성자 한정) — 로그인 필수 */
    @PatchMapping("/{id}")
    public ResponseEntity<MatePostResponse> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody MatePostUpdateRequest request
    ) {
        Long accountId = SecurityUtil.getCurrentAccountId();
        return ResponseEntity.ok(matePostService.update(accountId, id, request));
    }

    /** 3-5 모집글 삭제 (soft delete) — 로그인 필수 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        Long accountId = SecurityUtil.getCurrentAccountId();
        matePostService.softDelete(accountId, id);
        return ResponseEntity.noContent().build();
    }

    /** 3-8 모집글 수동 마감 (작성자 한정) — 로그인 필수 */
    @PatchMapping("/{id}/close")
    public ResponseEntity<Void> close(@PathVariable("id") Long id) {
        Long accountId = SecurityUtil.getCurrentAccountId();
        matePostService.close(accountId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 비로그인이 허용되는 엔드포인트에서 사용. 인증이 없으면 null 반환.
     */
    private Long getOptionalAccountId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
            return null;
        }
        try {
            return SecurityUtil.getCurrentAccountId();
        } catch (Exception e) {
            return null;
        }
    }
}
