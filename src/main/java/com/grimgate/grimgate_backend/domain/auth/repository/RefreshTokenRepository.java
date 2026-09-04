package com.grimgate.grimgate_backend.domain.auth.repository;

import com.grimgate.grimgate_backend.domain.auth.entity.RefreshToken;
import org.springframework.data.keyvalue.repository.KeyValueRepository;

public interface RefreshTokenRepository extends KeyValueRepository<RefreshToken, String> {
}
