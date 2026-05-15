package com.kid.A0.service.Interface

import com.kid.A0.dto.SubResponse

interface SubscriptionServiceInterface {

    fun createSub(username: String, planName: String): SubResponse

    fun getSub(username: String): SubResponse

}