package com.kid.A0.service;

import com.kid.A0.dto.PlanResponse;
import com.kid.A0.dto.PlanUpdate;
import com.kid.A0.model.Plan;
import com.kid.A0.repo.PlanRepo;
import com.kid.A0.service.Interface.PlanServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlanService implements PlanServiceInterface {

    private final PlanRepo planRepo;

    public PlanService(PlanRepo planRepo) {
        this.planRepo = planRepo;
    }

    public List<PlanResponse> getPlans() {
        return planRepo.findAll()
                .stream()
                .map(PlanResponse::new)
                .toList();
    }

    @Transactional
    public PlanResponse createPlan(PlanUpdate planUpdate) {
        boolean isExist = planRepo.existsPlanByName(planUpdate.name());
        if (isExist) throw new RuntimeException("plan already exist with this name");
        Plan plan = Plan.builder()
                .name(planUpdate.name())
                .monthlyPrice(planUpdate.monthlyPrice())
                .rateLimitPerMinute(planUpdate.rateLimitPerMinute())
                .isActive(true)
                .build();

        planRepo.save(plan);
        return new PlanResponse(plan);
    }

    public PlanResponse getPlan(long id) {
        Plan plan = planRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan not found"));
        return new PlanResponse(plan);
    }

    @Transactional
    public void deletePlan(long id) {
        Plan plan = planRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan not found"));
        plan.setActive(false);
        planRepo.save(plan);
    }

    @Transactional
    public PlanResponse updatePlan(long id, PlanUpdate planUpdate) {
        Plan plan = planRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan not found"));
        if (planUpdate.name() != null) {
            if (!plan.getName().equals(planUpdate.name()) && planRepo.existsPlanByName(planUpdate.name())) {
                throw new RuntimeException("Name already exist");
            }
            plan.setName(planUpdate.name());
        }

        if (planUpdate.monthlyPrice() != null) {
            plan.setMonthlyPrice(planUpdate.monthlyPrice());
        }
        if (planUpdate.rateLimitPerMinute() != null) {
            plan.setRateLimitPerMinute(planUpdate.rateLimitPerMinute());
        }
        planRepo.save(plan);
        return new PlanResponse(plan);
    }

    @Transactional
    public PlanResponse replacePlan(long id, PlanUpdate planUpdate) {
        Plan plan = planRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        if (!plan.getName().equals(planUpdate.name()) && planRepo.existsPlanByName(planUpdate.name())) {
            throw new RuntimeException("Name already exist");
        }

        plan.setName(planUpdate.name());
        plan.setMonthlyPrice(planUpdate.monthlyPrice());
        plan.setRateLimitPerMinute(planUpdate.rateLimitPerMinute());
        planRepo.save(plan);
        return new PlanResponse(plan);
    }
}
