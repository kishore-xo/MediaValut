package com.kid.A0.dto;


import com.kid.A0.model.Role;
import com.kid.A0.model.User;

import java.io.Serializable;

public record UserResponse(Long id, String username, String email, Role role) implements Serializable {
    public UserResponse(User user) {
        this(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }
}
