package com.grimgate.grimgate_backend.domain.ai.dto;

import java.util.List;

public record AiRecommendRequest(
        List<Message> messages
) {
    public enum Role {
        user, assistant
    }

    public record Message(
            Role role,
            String content
    ) {}
}