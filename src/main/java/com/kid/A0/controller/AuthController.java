package com.kid.A0.controller;

import com.kid.A0.dto.LoginResponse;
import com.kid.A0.dto.UserRequest;
import com.kid.A0.dto.UserResponse;
import com.kid.A0.model.User;
import com.kid.A0.repo.UserRepo;
import com.kid.A0.security.JwtUtil;
import com.kid.A0.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserRepo userRepo;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthController(UserRepo userRepo, JwtUtil jwtUtil, UserService userService, BCryptPasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestParam String username, @RequestParam String password) {
        User user = userRepo.findUserByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User Not Found"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        String token = jwtUtil.generateToken(String.valueOf(user.getId()));
        ResponseCookie cookie = ResponseCookie.from("a0_token", token)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofHours(1))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new LoginResponse(user.getId(), user.getUsername(), user.getRole(), token, "Bearer"));
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRequest userRequest) {
        UserResponse response = userService.createUser(userRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        ResponseCookie cookie = ResponseCookie.from("a0_token", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body("Logout");
    }
}
