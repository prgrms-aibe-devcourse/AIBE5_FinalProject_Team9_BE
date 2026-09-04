package com.grimgate.grimgate_backend.global.security;

import com.grimgate.grimgate_backend.global.exception.CustomException;
import com.grimgate.grimgate_backend.global.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

// SecurityContext에서 현재 로그인 사용자 정보를 추출하는 유틸 클래스
public class SecurityUtil {

    // 인스턴스화 불가
    private SecurityUtil() {}

    /**
     * SecurityContext에서 현재 로그인된 사용자의 accountId를 반환합니다.
     *
     * @return 현재 로그인 사용자의 accountId (Long)
     * @throws CustomException Authentication이 없거나 인증되지 않은 경우, principal 타입이 다른 경우
     */
    public static Long getCurrentAccountId() {
        // Authentication 객체 조회
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Authentication이 null이거나 인증되지 않은 상태이면 예외
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();

        // principal이 UserDetails 타입인 경우 getUsername()을 Long으로 변환하여 반환
        if (principal instanceof UserDetails userDetails) {
            try {
                return Long.parseLong(userDetails.getUsername());
            } catch (NumberFormatException e) {
                // username이 숫자로 변환 불가한 경우
                throw new CustomException(ErrorCode.UNAUTHORIZED);
            }
        }

        // principal이 String이거나 예상과 다른 타입인 경우
        throw new CustomException(ErrorCode.UNAUTHORIZED);
    }
}
