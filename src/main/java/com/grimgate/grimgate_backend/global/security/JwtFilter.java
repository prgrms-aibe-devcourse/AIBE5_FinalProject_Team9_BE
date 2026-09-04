package com.grimgate.grimgate_backend.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// 요청 JWT 인증 처리
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token) && jwtProvider.validateToken(token)) {
            // 블랙리스트에 등록된 Access Token이면 401 반환
            if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token))) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"message\":\"로그아웃된 토큰입니다.\",\"data\":null}");
                response.getWriter().flush();
                return;
            }

            Long accountId = jwtProvider.getAccountId(token);

            // CustomUserDetailsService를 통해 UserDetails 로드 (탈퇴 계정 제외 처리 포함)
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(String.valueOf(accountId));

            // principal을 UserDetails 객체로 설정
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    // Authorization 헤더에서 Bearer 토큰 추출
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
