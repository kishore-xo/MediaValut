package com.kid.A0.service.Interface;

import com.kid.A0.dto.ApiKeyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ApiKeyServiceInterface {

    ApiKeyResponse createKey(String username, String keyName);

    Page<ApiKeyResponse> getApiKeys(String username, Pageable pageable);

    void revokeKey(String username, Long keyId);

}
