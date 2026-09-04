package com.grimgate.grimgate_backend.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

// JWT 생성 / 검증 / 정보 추출
@Slf4j
@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secretKeyBase64;

    @Value("${jwt.access-token-validity-ms}")
    private long accessTokenValidityMs;

    @Value("${jwt.refresh-token-validity-ms}")
    private long refreshTokenValidityMs;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Base64.getDecoder().decode(secretKeyBase64);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    // Access Token 생성 (만료: 설정값, 기본 30분)
    public String generateAccessToken(Long accountId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenValidityMs);

        return Jwts.builder()
                .subject(String.valueOf(accountId))
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    // Refresh Token 생성 (만료: 7일)
    public String generateRefreshToken(Long accountId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenValidityMs);

        return Jwts.builder()
                .subject(String.valueOf(accountId))
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("유효하지 않은 JWT 토큰: {}", e.getMessage());
            return false;
        }
    }

    // 토큰에서 accountId 추출
    public Long getAccountId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    // 토큰에서 role 추출
    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    // 토큰의 남은 만료 시간(ms) 반환 — 이미 만료된 경우 0 이하 반환
    public long getRemainingExpiration(String token) {
        try {
            Date expiration = parseClaims(token).getExpiration();
            return expiration.getTime() - System.currentTimeMillis();
        } catch (JwtException | IllegalArgumentException e) {
            return 0;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
