package com.kid.A0.service.Interface;

import com.kid.A0.dto.ApiKeyResponse;

import java.util.List;

public interface ApiKeyServiceInterface {

    ApiKeyResponse createKey(Long userId, String keyName);

    List<ApiKeyResponse> getApiKeys(Long userId);

    void revokeKey(Long userId, Long keyId);

}
