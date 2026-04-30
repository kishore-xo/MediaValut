package com.kid.A0.dto;

import com.kid.A0.model.ApiKey;

import java.time.LocalDateTime;

public record ApiKeyResponse
        (Long id, String prefix,
         String rawKey,
         String name, LocalDateTime createdAt,
         LocalDateTime expiresAt, LocalDateTime lastUsed,
         boolean revoked, Long userId) {

    public ApiKeyResponse(String rawKey, ApiKey apiKey) {
        this(apiKey.getId(), apiKey.getPrefix(), rawKey,
                apiKey.getName(), apiKey.getCreatedAt(),
                apiKey.getExpiresAt(), apiKey.getLastUsed(),
                apiKey.isRevoked(), apiKey.getUser().getId());
    }

    public ApiKeyResponse(ApiKey apiKey) {
        this(apiKey.getId(), apiKey.getPrefix(), null,
                apiKey.getName(), apiKey.getCreatedAt(),
                apiKey.getExpiresAt(), apiKey.getLastUsed(),
                apiKey.isRevoked(), apiKey.getUser().getId());
    }
}
