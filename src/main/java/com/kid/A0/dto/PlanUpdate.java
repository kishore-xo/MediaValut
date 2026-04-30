package com.kid.A0.dto;

import java.math.BigDecimal;

public record PlanUpdate
        (
                String name,
                BigDecimal monthlyPrice,
                Long rateLimitPerMinute
        )
{}
