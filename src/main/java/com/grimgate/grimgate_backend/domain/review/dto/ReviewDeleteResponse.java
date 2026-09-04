package com.grimgate.grimgate_backend.domain.review.dto;

import java.time.LocalDateTime;

public record ReviewDeleteResponse(
        Long id,
        LocalDateTime deletedAt
) {}