package com.kid.A0.dto;

import com.kid.A0.model.SubscribeStatus;
import com.kid.A0.model.Subscription;

import java.time.LocalDate;

public record SubResponse(Long id, String planName, Long userId, Long planId, LocalDate startDate,
                          LocalDate endDate, SubscribeStatus status) {

    public SubResponse(Subscription subscription) {
        this(subscription.getId(), subscription.getPlan().getName(), subscription.getUser().getId(), subscription.getPlan().getId(),
                subscription.getStartDate(), subscription.getEndDate(), subscription.getStatus());
    }
}
