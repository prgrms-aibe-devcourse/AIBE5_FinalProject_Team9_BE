package com.grimgate.grimgate_backend.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// Spring Security 설정
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final StringRedisTemplate redisTemplate;

    // 비밀번호 암호화에 사용할 BCrypt 인코더
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                // 세션 사용 안 함 (STATELESS)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // CORS는 WebConfig 설정 따름
                .cors(cors -> cors.configure(http))

                .authorizeHttpRequests(auth -> auth
                        

                        // Auth - 인증 불필요 엔드포인트
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/register/member",
                                "/api/auth/register/manager",
                                "/api/auth/login/member",
                                "/api/auth/login/manager",
                                "/api/auth/login/admin",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/api/auth/oauth/google",
                                "/api/auth/password/reset-request",
                                "/api/auth/password/reset"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/auth/check-email",
                                "/api/auth/check-nickname"
                        ).permitAll()

                        .requestMatchers("/api/s3/**").permitAll()

                        // Theme - 조회는 인증 불필요
                        .requestMatchers(HttpMethod.GET,
                                "/api/themes/**",
                                "/api/themes/popular",
                                "/api/themes/{id}",
                                "/api/themes/{id}/reviews",
                                "/api/themes/{id}/slots",
                                "/api/themes/{id}/age-check"
                        ).permitAll()

                        // Branch - 조회는 인증 불필요
                        .requestMatchers(HttpMethod.GET,
                                "/api/branches",
                                "/api/branches/{id}",
                                "/api/branches/{id}/themes"
                        ).permitAll()

                        // Slot - 가용 슬롯 조회는 인증 불필요
                        .requestMatchers(HttpMethod.GET, "/api/slots/available").permitAll()

                        // Mate Post - 조회는 인증 불필요
                        .requestMatchers(HttpMethod.GET,
                                "/api/mate-posts",
                                "/api/mate-posts/{id}",
                                "/api/mate-posts/{id}/comments",
                                "/api/mate-posts/stats"
                        ).permitAll()

                        // Review - 단건 조회는 인증 불필요
                        .requestMatchers(HttpMethod.GET, "/api/reviews/{id}").permitAll()

                        // 공통 조회 - 인증 불필요
                        .requestMatchers(HttpMethod.GET,
                                "/api/profile-characters",
                                "/api/titles",
                                "/api/achievements"
                        ).permitAll()

                        // AI 추천 - 인증 불필요
                        .requestMatchers(HttpMethod.POST, "/api/ai/recommend").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/ai/recommend/random").permitAll()

                        // Admin - ADMIN 역할만 접근 가능
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Owner - MANAGER 역할만 접근 가능
                        .requestMatchers("/api/owner/**").hasRole("MANAGER")

                        // 나머지 모든 요청은 인증 필요 (/api/auth/logout 포함)
                        .anyRequest().authenticated()
                )

                // 인증/권한 예외 처리 핸들러 연결
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )

                .formLogin(form -> form.disable())

                // JwtFilter를 UsernamePasswordAuthenticationFilter 앞에 등록
                .addFilterBefore(new JwtFilter(jwtProvider, customUserDetailsService, redisTemplate), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
