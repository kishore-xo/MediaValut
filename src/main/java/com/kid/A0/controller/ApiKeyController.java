package com.kid.A0.controller;

import com.kid.A0.dto.ApiKeyResponse;
import com.kid.A0.service.ApiKeyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/apikey")
public class ApiKeyController {
    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping("/{keyName}")
    public ResponseEntity<ApiKeyResponse> createKey(Principal principal, @PathVariable String keyName) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiKeyService.createKey(principal.getName(), keyName));
    }

    @GetMapping
    public ResponseEntity<Page<ApiKeyResponse>> getApiKeys(Principal principal,
                                                           @PageableDefault(size = 3) Pageable pageable) {
        return ResponseEntity.ok(apiKeyService.getApiKeys(principal.getName(), pageable));
    }

    @DeleteMapping("/{keyId}")
    public ResponseEntity<Void> deleteApiKey(Principal principal, @PathVariable Long keyId) {
        apiKeyService.revokeKey(principal.getName(), keyId);
        return ResponseEntity.noContent().build();
    }
}
