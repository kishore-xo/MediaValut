package com.kid.A0.controller;

import com.kid.A0.dto.SubResponse;
import com.kid.A0.model.Subscription;
import com.kid.A0.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/sub")
public class SubscriptionController {
    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public ResponseEntity<SubResponse> createSub(Principal principal, @RequestParam String planName) {
        SubResponse sub = subscriptionService.createSub(principal.getName(), planName);
        return ResponseEntity.ok(sub);
    }

    @GetMapping("/current")
    public ResponseEntity<SubResponse> getSub(Principal principal) {
        return ResponseEntity.ok(subscriptionService.getSub(principal.getName()));
    }
}
