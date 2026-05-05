package com.kid.A0.service;

import com.kid.A0.dto.SubResponse;
import com.kid.A0.model.Plan;
import com.kid.A0.model.SubscribeStatus;
import com.kid.A0.model.Subscription;
import com.kid.A0.model.User;
import com.kid.A0.repo.PlanRepo;
import com.kid.A0.repo.SubscriptionRepo;
import com.kid.A0.repo.UserRepo;
import com.kid.A0.service.Interface.SubscriptionServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class SubscriptionService implements SubscriptionServiceInterface {
    private final SubscriptionRepo subscriptionRepo;
    private final PlanRepo planRepo;
    private final UserRepo userRepo;

    public SubscriptionService(SubscriptionRepo subscriptionRepo, PlanRepo planRepo, UserRepo userRepo) {
        this.subscriptionRepo = subscriptionRepo;
        this.planRepo = planRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public SubResponse createSub(long userId, String planName) {
        Plan plan = planRepo.findByName(planName)
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        Subscription subscription = subscriptionRepo.findByUserIdAndStatus(userId, SubscribeStatus.ACTIVE)
                .orElse(new Subscription());
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User Not found"));


        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusMonths(1));
        subscription.setStatus(SubscribeStatus.ACTIVE);

        subscriptionRepo.save(subscription);
        return new SubResponse(subscription);
    }

    public SubResponse getSub(long userId) {
        Subscription subscription = subscriptionRepo.findByUserIdAndStatus(userId, SubscribeStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("No active subscription found for this user"));
        return new SubResponse(subscription);
    }

    public long getRateLimit(long userId){
        Subscription subscription = subscriptionRepo.findByUserIdAndStatus(userId,SubscribeStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("No active subscription found for this user"));
        return subscription.getPlan().getRateLimitPerMinute();
    }

}
