package com.kid.A0.service.Interface;

import com.kid.A0.dto.ApiKeyResponse;

import java.util.List;

public interface ApiKeyServiceInterface {

    ApiKeyResponse createKey(String username, String keyName);

    List<ApiKeyResponse> getApiKeys(String username);

    void revokeKey(String username, Long keyId);

}
