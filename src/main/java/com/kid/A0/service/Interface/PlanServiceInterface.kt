package com.kid.A0.service.Interface

import com.kid.A0.dto.PlanResponse
import com.kid.A0.dto.PlanUpdate

interface PlanServiceInterface {

    val plans: List<PlanResponse>

    fun createPlan(planUpdate: PlanUpdate): PlanResponse

    fun getPlan(id: Long): PlanResponse

    fun deletePlan(id: Long)

    fun updatePlan(id: Long, planUpdate: PlanUpdate): PlanResponse

    fun replacePlan(id: Long, planUpdate: PlanUpdate): PlanResponse
}