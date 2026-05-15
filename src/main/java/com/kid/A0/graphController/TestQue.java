package com.kid.A0.graphController;

import com.kid.A0.dto.*;
import com.kid.A0.model.Role;
import com.kid.A0.service.*;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Controller
public class TestQue {


    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final ApiKeyService apiKeyService;
    private final PlanService planService;
    private final VideoService videoService;
    private final PhotoService photoService;

    // Use a dedicated executor for I/O bound tasks to avoid starving the common ForkJoinPool
    private final Executor executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

    public TestQue(UserService userService, SubscriptionService subscriptionService, ApiKeyService apiKeyService, PlanService planService, VideoService videoService, PhotoService photoService) {
        this.userService = userService;
        this.subscriptionService = subscriptionService;
        this.apiKeyService = apiKeyService;
        this.planService = planService;
        this.videoService = videoService;
        this.photoService = photoService;
    }

    @QueryMapping(name = "getUser")
    public UserResponse getUser(@Argument Long id, Principal principal) {
        validUser(id, principal);
        return userService.getUser(id);
    }

    @SchemaMapping(typeName = "UserResponseGp", field = "subResponse")
    public CompletableFuture<SubResponse> subResponse(UserResponse user) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return subscriptionService.getSub(user.username());
            } catch (Exception e) {
                return null; // Return null if no subscription exists
            }
        }, executor);
    }

    @SchemaMapping(typeName = "UserResponseGp", field = "planResponse")
    public CompletableFuture<PlanResponse> planResponse(UserResponse user) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                SubResponse sub = subscriptionService.getSub(user.username());
                if (sub != null && sub.planId() != null) {
                    return planService.getPlan(sub.planId());
                }
                return null;
            } catch (Exception e) {
                return null;
            }
        }, executor);
    }

    @SchemaMapping(typeName = "UserResponseGp", field = "apiKeyResponse")
    public CompletableFuture<List<ApiKeyResponse>> apiKeyResponse(UserResponse user) {
        return CompletableFuture.supplyAsync(() -> apiKeyService.getApiKeys(user.username()), executor);
    }

    @SchemaMapping(typeName = "UserResponseGp", field = "videoResponse")
    public CompletableFuture<List<MediaResponse>> videoResponse(UserResponse user, Principal principal) {
        return CompletableFuture.supplyAsync(() -> videoService.getVideos(user.username()), executor);
    }

    @SchemaMapping(typeName = "UserResponseGp", field = "photoResponse")
    public CompletableFuture<List<MediaResponse>> photoResponse(UserResponse user, Principal principal) {
        return CompletableFuture.supplyAsync(() -> photoService.getPhotos(user.username()), executor);
    }

    private void validUser(Long id, Principal principal) {
        if (principal == null) throw new RuntimeException("Unauthorized");
        UserResponse current = userService.getMe(principal.getName());
        if (!Role.ADMIN.equals(current.role()) && !current.id().equals(id)) throw new RuntimeException("Forbidden");
    }
}
