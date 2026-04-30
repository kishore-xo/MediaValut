package com.kid.A0.graphController;

import com.kid.A0.dto.*;
import com.kid.A0.model.Role;

import java.util.List;

public record UserResponseGp
        (
                Long id,
                String username,
                String email,
                Role role,
                SubResponse subResponse,
                List<ApiKeyResponse> apiKeyResponse,
                PlanResponse planResponse,
                List<MediaResponse> videoResponse,
                List<MediaResponse> photoResponse

        ) {

    public UserResponseGp(UserResponse user, SubResponse subResponse, List<ApiKeyResponse> apiKeyResponse
            , PlanResponse planResponse, List<MediaResponse> videoResponse,List<MediaResponse> photoResponse) {
        this(user.id(), user.username(), user.email(), user.role(),
                subResponse, apiKeyResponse, planResponse, videoResponse,photoResponse);
    }
}
