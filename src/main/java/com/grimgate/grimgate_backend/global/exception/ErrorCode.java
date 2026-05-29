package com.grimgate.grimgate_backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// 서비스 전반에서 사용하는 에러 코드 목록
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 인증/계정
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // 토큰
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    REVOKED_TOKEN(HttpStatus.UNAUTHORIZED, "이미 로그아웃된 토큰입니다."),

    // 회원/역할
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "멤버 정보를 찾을 수 없습니다."),
    MANAGER_NOT_FOUND(HttpStatus.NOT_FOUND, "매니저 정보를 찾을 수 없습니다."),
    INVALID_ROLE(HttpStatus.BAD_REQUEST, "유효하지 않은 역할입니다."),

    // 프로필/마이페이지
    PROFILE_CHARACTER_NOT_FOUND(HttpStatus.NOT_FOUND, "프로필 캐릭터를 찾을 수 없습니다."),
    TITLE_NOT_FOUND(HttpStatus.NOT_FOUND, "칭호를 찾을 수 없습니다."),

    // 지점/테마 (사장님 페이지)
    BRANCH_NOT_FOUND(HttpStatus.NOT_FOUND, "지점을 찾을 수 없습니다."),
    BRANCH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 지점에 대한 권한이 없습니다."),
    THEME_NOT_FOUND(HttpStatus.NOT_FOUND, "테마를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
