package com.kid.A0.service.Interface

import com.kid.A0.dto.SubResponse

interface SubscriptionServiceInterface {

    fun createSub(userId: Long, planName: String): SubResponse

    fun getSub(userId: Long): SubResponse

}