package com.kid.A0.controller;

import com.kid.A0.dto.PlanResponse;
import com.kid.A0.dto.PlanUpdate;
import com.kid.A0.service.PlanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plan")
public class PlanController {
    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @PreAuthorize("permitAll()")
    @GetMapping
    public ResponseEntity<List<PlanResponse>> getPlans() {
        return ResponseEntity.ok(planService.getPlans());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<PlanResponse> getPlan(@PathVariable Long id) {
        return ResponseEntity.ok(planService.getPlan(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<PlanResponse> createPlan(@RequestBody PlanUpdate planUpdate) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(planService.createPlan(planUpdate));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePlan(@PathVariable Long id) {
        planService.deletePlan(id);
        return ResponseEntity.ok("Deleted");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<?> updatePlan(@PathVariable Long id,
                                        @RequestBody PlanUpdate planUpdate) {
        return ResponseEntity.ok(planService.updatePlan(id, planUpdate));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<PlanResponse> replacePlan(@PathVariable Long id,
                                                    @RequestBody PlanUpdate planUpdate) {
        return ResponseEntity.ok(planService.replacePlan(id, planUpdate));
    }
}
