package com.kid.A0.service;

import com.kid.A0.dto.ApiKeyResponse;
import com.kid.A0.model.ApiKey;
import com.kid.A0.model.User;
import com.kid.A0.repo.ApiKeyRepo;
import com.kid.A0.repo.UserRepo;
import com.kid.A0.service.Interface.ApiKeyServiceInterface;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class ApiKeyService implements ApiKeyServiceInterface {

    private final ApiKeyRepo apiKeyRepo;
    private final UserRepo userRepo;
    private final String CACHE_NAME = "api_key";

    public ApiKeyService(ApiKeyRepo apiKeyRepo, UserRepo userRepo) {
        this.apiKeyRepo = apiKeyRepo;
        this.userRepo = userRepo;
    }

    @CachePut(cacheNames =CACHE_NAME,key = "#result.id()")
    public ApiKeyResponse createKey(String username, String keyName) {
        String rawKey = "water-law" + UUID.randomUUID().toString().replace("-", "");
        String hashKey = hash(rawKey);

        User user = currentUser(username);
        ApiKey apiKey = ApiKey.builder()
                .user(user)
                .hashedKey(hashKey)
                .name(keyName)
                .prefix(rawKey.substring(0, 12))
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusMonths(1))
                .build();
        ApiKey savedKey = apiKeyRepo.save(apiKey);
        return new ApiKeyResponse(rawKey, savedKey);
    }

    public String hash(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash key");
        }
    }

    @Transactional
    public Page<ApiKeyResponse> getApiKeys(String username, Pageable pageable) {
        User user = currentUser(username);
        Page<ApiKey> apiKeys = apiKeyRepo.findByUserIdAndRevokedFalse(user.getId(),pageable);

        return apiKeys.map(ApiKeyResponse::new);
    }

    public void revokeKey(String username, Long keyId) {
        User user = currentUser(username);
        ApiKey apiKey = apiKeyRepo.findByUserIdAndId(user.getId(), keyId)
                .orElseThrow(() -> new RuntimeException("key not found or unauthorized"));
        if (apiKey.isRevoked()) {
            throw new RuntimeException("Key already Revoked");
        }
        apiKey.setRevoked(true);
        apiKeyRepo.save(apiKey);
    }

    private User currentUser(String username) {
        return userRepo.findUserByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found"));
    }
}
