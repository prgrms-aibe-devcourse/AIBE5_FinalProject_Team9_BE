package com.grimgate.grimgate_backend.domain.auth.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.time.LocalDateTime;

@RedisHash(value = "refresh_token", timeToLive = 604800)
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class RefreshToken {

    @Id
    private String id;

    private Long accountId;

    private String token;

    private LocalDateTime expiredAt;
}
