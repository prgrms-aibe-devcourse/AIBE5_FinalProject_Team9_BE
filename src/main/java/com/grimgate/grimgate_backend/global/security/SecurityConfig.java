package com.grimgate.grimgate_backend.global.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

//Spring Security 설정
@Configuration
@EnableWebSecurity
public class SecurityConfig {

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    http
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            "/swagger-ui/**",
                            "/v3/api-docs/**"
                    ).permitAll()

                    .anyRequest().permitAll()
            )

            .formLogin(form -> form.disable());

    return http.build();
}
}
