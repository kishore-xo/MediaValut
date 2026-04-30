package com.kid.A0.aspect;

import com.kid.A0.exception.RateLimitException;
import com.kid.A0.model.ApiKey;
import com.kid.A0.repo.ApiKeyRepo;
import com.kid.A0.service.ApiKeyService;
import com.kid.A0.service.RedisRateLimit;
import com.kid.A0.service.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;

@Aspect
@Component
public class RateLimitAspect {

    private final RedisRateLimit redisRateLimit;
    private final ApiKeyService apiKeyService;
    private final SubscriptionService subscriptionService;
    private final ApiKeyRepo apiKeyRepo;
    private final StringRedisTemplate redisTemplate;

    @Value("${customKey}")
    private String SECRET_KEY;


    public RateLimitAspect
            (
                    RedisRateLimit redisRateLimit, ApiKeyService apiKeyService,
                    SubscriptionService subscriptionService,
                    ApiKeyRepo apiKeyRepo, StringRedisTemplate redisTemplate
            ) {
        this.redisRateLimit = redisRateLimit;
        this.apiKeyService = apiKeyService;
        this.subscriptionService = subscriptionService;
        this.apiKeyRepo = apiKeyRepo;
        this.redisTemplate = redisTemplate;
    }

    @Around("@annotation(com.kid.A0.annotation.RateLimit)||@within(com.kid.A0.annotation.RateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint) throws Throwable {

        HttpServletRequest servletRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        String rawKey = firstNonBlank(
                servletRequest.getHeader("X-API-Key"),
                servletRequest.getHeader("apikey"),
                servletRequest.getParameter("apikey")
        );

        if (SECRET_KEY.equals(rawKey)) {
            return joinPoint.proceed();
        }

        if (rawKey == null || rawKey.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized: Missing api-key");
        }

        String hashKey = apiKeyService.hash(rawKey);

        String cachedMeta = redisTemplate.opsForValue().get("meta:" + hashKey);
        long userId;
        long limit;

        if (cachedMeta == null) {

            ApiKey apiKey = apiKeyRepo.findByHashedKey(hashKey)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized: Invalid API Key"));
            if (apiKey.isRevoked()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized: API Key has been revoked");
            }
            userId = apiKey.getUser().getId();
            limit = subscriptionService.getRateLimit(apiKey.getUser().getId());

            redisTemplate.opsForValue().set("meta:" + hashKey, userId + ":" + limit, Duration.ofMinutes(5));

            if (apiKey.getLastUsed() == null || apiKey.getLastUsed().isBefore(LocalDateTime.now().minusHours(1))) {
                apiKey.setLastUsed(LocalDateTime.now());
                apiKeyRepo.save(apiKey);
            }
        } else {
            String[] parts = cachedMeta.split(":");
            userId = Long.parseLong(parts[0]);
            limit = Integer.parseInt(parts[1]);
        }

        if (!redisRateLimit.isAllowed("user:" + userId, (int) limit)) {
            throw new RateLimitException("Rate limit exceeded. Please upgrade your plan.");
        }
        return joinPoint.proceed();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
