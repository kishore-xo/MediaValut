package com.kid.A0.service.Interface;

import com.kid.A0.dto.SubResponse;

public interface SubscriptionServiceInterface {

    SubResponse createSub(String username, String planName);

    SubResponse getSub(String username);
}