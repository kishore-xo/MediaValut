package com.kid.A0.controller;

import com.kid.A0.dto.UserRequest;
import com.kid.A0.dto.UserResponse;
import com.kid.A0.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<UserResponse>> getUsers() {
        return new ResponseEntity<>(userService.getUsers(), HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return new ResponseEntity<>(userService.getUser(id), HttpStatus.OK);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Principal principal) {
        Long userId = Long.parseLong(principal.getName());

        return ResponseEntity.ok(userService.getMe(userId));
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest userRequest) {
        return new ResponseEntity<>(userService.createUser(userRequest), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.deleteUser(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(Principal principal,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody UserRequest userRequest) {
        return ResponseEntity.ok(userService.updateUser(Long.parseLong(principal.getName()), id, userRequest));
    }

}
