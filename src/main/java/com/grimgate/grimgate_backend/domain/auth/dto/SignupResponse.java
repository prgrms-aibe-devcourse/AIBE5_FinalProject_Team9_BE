package com.grimgate.grimgate_backend.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SignupResponse {

    // 계정 정보
    private Long id;
    private String nickname;
    private String email;
    private LocalDateTime createdAt;

    // 발급 토큰 정보
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
}
