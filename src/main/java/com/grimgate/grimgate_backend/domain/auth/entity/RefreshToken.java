package com.grimgate.grimgate_backend.domain.auth.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.time.LocalDateTime;

@RedisHash(value = "refresh_token")
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

    // rememberMe 여부에 따라 동적으로 TTL 적용 (초 단위)
    @TimeToLive
    private Long ttl;
}
