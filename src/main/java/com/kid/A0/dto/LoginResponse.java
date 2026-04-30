package com.kid.A0.dto;

import com.kid.A0.model.Role;

import java.io.Serializable;

public record LoginResponse(Long userId, String username, Role role, String token, String tokenType) implements Serializable {
}
