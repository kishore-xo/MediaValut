package com.kid.A0.service.Interface;

import com.kid.A0.dto.PlanResponse;
import com.kid.A0.dto.PlanUpdate;

import java.util.List;

public interface PlanServiceInterface {

    List<PlanResponse> getPlans();

    PlanResponse createPlan(PlanUpdate planUpdate);

    PlanResponse getPlan(long id);

    void deletePlan(long id);

    PlanResponse updatePlan(long id, PlanUpdate planUpdate);

    PlanResponse replacePlan(long id, PlanUpdate planUpdate);
}