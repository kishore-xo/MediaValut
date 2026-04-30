package com.kid.A0.dto;

import com.kid.A0.model.Plan;
import org.springframework.util.unit.DataSize;

import java.math.BigDecimal;

public record PlanResponse
        (
                Long id,
                String name,
                BigDecimal monthlyPrice,
                long rateLimitPerMinute,
                boolean isActive,
                int mediaCount,
                String photoSize,
                String videoSize
        ) {
    public PlanResponse(Plan plan) {

        this(plan.getId(), plan.getName(), plan.getMonthlyPrice(),
                plan.getRateLimitPerMinute(), plan.isActive(), plan.getMediaCount(),
                plan.getPhotoSize(),plan.getVideoSize());
    }
}
