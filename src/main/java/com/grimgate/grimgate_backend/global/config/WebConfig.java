package com.grimgate.grimgate_backend.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Next.js 연동 허용 설정 및 웹 공통 설정
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // 모든 경로에 대해 CORS 설정 적용
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String allowedOrigin = System.getenv("ALLOWED_ORIGIN");

        // 환경변수 설정 시 해당 Origin만 허용, 미설정 시 로컬 개발 서버 허용
        if (allowedOrigin != null && !allowedOrigin.isBlank()) {
            registry.addMapping("/**")
                    .allowedOrigins(allowedOrigin)
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")  // 허용 HTTP 메서드
                    .allowedHeaders("*")                           // 모든 헤더 허용
                    .allowCredentials(true)                        // 쿠키/인증 정보 포함 요청 허용
                    .maxAge(3600);                                 // 프리플라이트 캐시 시간 (초)
        } else {
            registry.addMapping("/**")
                    .allowedOrigins("http://localhost:3000")       // Next.js 개발 서버 허용
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")  // 허용 HTTP 메서드
                    .allowedHeaders("*")                           // 모든 헤더 허용
                    .allowCredentials(true)                        // 쿠키/인증 정보 포함 요청 허용
                    .maxAge(3600);                                 // 프리플라이트 캐시 시간 (초)
        }
    }
}
