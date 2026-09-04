package com.grimgate.grimgate_backend.domain.auth.controller;

import com.grimgate.grimgate_backend.domain.account.entity.Role;
import com.grimgate.grimgate_backend.domain.auth.dto.*;
import com.grimgate.grimgate_backend.domain.auth.service.AuthService;
import com.grimgate.grimgate_backend.global.response.ApiResponse;
import com.grimgate.grimgate_backend.global.security.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 일반 회원 가입 (MEMBER 고정)
    @PostMapping("/register/member")
    public ResponseEntity<ApiResponse<SignupResponse>> signupMember(
            @RequestBody @Valid SignupRequest request) {
        SignupResponse response = authService.signup(request, Role.MEMBER);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다.", response));
    }

    // 매니저 회원 가입 (MANAGER 고정, 지점 정보 포함)
    @PostMapping("/register/manager")
    public ResponseEntity<ApiResponse<SignupResponse>> signupManager(
            @RequestBody @Valid ManagerSignupRequest request) {
        SignupResponse response = authService.signupManager(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다.", response));
    }

    // 일반 회원 로그인 (MEMBER 고정)
    @PostMapping("/login/member")
    public ResponseEntity<ApiResponse<TokenResponse>> loginMember(
            @RequestBody @Valid LoginRequest request) {
        TokenResponse response = authService.login(request, Role.MEMBER);
        return ResponseEntity.ok(ApiResponse.success("로그인이 완료되었습니다.", response));
    }

    // 매니저 로그인 (MANAGER 고정)
    @PostMapping("/login/manager")
    public ResponseEntity<ApiResponse<TokenResponse>> loginManager(
            @RequestBody @Valid LoginRequest request) {
        TokenResponse response = authService.login(request, Role.MANAGER);
        return ResponseEntity.ok(ApiResponse.success("로그인이 완료되었습니다.", response));
    }

    // 관리자 로그인 (ADMIN 고정)
    @PostMapping("/login/admin")
    public ResponseEntity<ApiResponse<TokenResponse>> loginAdmin(
            @RequestBody @Valid LoginRequest request) {
        TokenResponse response = authService.login(request, Role.ADMIN);
        return ResponseEntity.ok(ApiResponse.success("로그인이 완료되었습니다.", response));
    }

    // 토큰 재발급
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @RequestBody @Valid RefreshTokenRequest request) {
        TokenResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success("토큰이 재발급되었습니다.", response));
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @RequestBody @Valid RefreshTokenRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        // Authorization 헤더에서 "Bearer " 접두사 제거 후 Access Token 추출
        String accessToken = null;
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            accessToken = authorizationHeader.substring(7);
        }
        String message = authService.logout(request, accessToken);
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }

    // 이메일 중복 확인
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Void>> checkEmail(@RequestParam String email) {
        authService.checkEmail(email);
        return ResponseEntity.ok(ApiResponse.success("사용 가능한 이메일입니다.", null));
    }

    // 닉네임 중복 확인
    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<Void>> checkNickname(@RequestParam String nickname) {
        authService.checkNickname(nickname);
        return ResponseEntity.ok(ApiResponse.success("사용 가능한 닉네임입니다.", null));
    }

    // 회원 탈퇴
    @DeleteMapping("/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        // SecurityContext에서 현재 로그인 사용자의 accountId 추출
        Long accountId = SecurityUtil.getCurrentAccountId();

        // Authorization 헤더에서 "Bearer " 접두사 제거 후 Access Token 추출 (logout() 패턴 동일)
        String accessToken = null;
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            accessToken = authorizationHeader.substring(7);
        }

        authService.withdraw(accountId, accessToken);
        return ResponseEntity.ok(ApiResponse.success("회원 탈퇴가 완료되었습니다.", null));
    }

    // 내 정보 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeResponse>> getMe() {
        MeResponse response = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("내 정보를 조회했습니다.", response));
    }

    // 비밀번호 변경
    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody @Valid ChangePasswordRequest request) {
        Long accountId = SecurityUtil.getCurrentAccountId();
        authService.changePassword(accountId, request);
        return ResponseEntity.ok(ApiResponse.success("비밀번호가 변경되었습니다.", null));
    }

    // TODO: AU-003 POST /api/auth/oauth/google (Google OAuth 소셜 로그인)
    // TODO: AU-006 POST /api/auth/password/reset-request (비밀번호 재설정 이메일 발송)
    // TODO: AU-007 POST /api/auth/password/reset (비밀번호 재설정)
}
