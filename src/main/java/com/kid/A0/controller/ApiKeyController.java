package com.kid.A0.controller;

import com.kid.A0.dto.ApiKeyResponse;
import com.kid.A0.service.ApiKeyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/apikey")
public class ApiKeyController {
    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping("/{keyName}")
    public ResponseEntity<ApiKeyResponse> createKey(Principal principal, @PathVariable String keyName) {
        Long userId = Long.parseLong(principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiKeyService.createKey(userId, keyName));
    }

    @GetMapping
    public ResponseEntity<List<ApiKeyResponse>> getApiKeys(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        return ResponseEntity.ok(apiKeyService.getApiKeys(userId));
    }

    @DeleteMapping("/{keyId}")
    public ResponseEntity<Void> deleteApiKey(Principal principal, @PathVariable Long keyId) {
        Long userId = Long.parseLong(principal.getName());
        apiKeyService.revokeKey(userId, keyId);
        return ResponseEntity.noContent().build();
    }
}
