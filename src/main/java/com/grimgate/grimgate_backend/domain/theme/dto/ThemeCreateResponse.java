package com.grimgate.grimgate_backend.domain.theme.dto;

import java.time.LocalDateTime;

public record ThemeCreateResponse(
        Long id,
        LocalDateTime createdAt
) {}