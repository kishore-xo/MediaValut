package com.kid.A0.service;

import com.kid.A0.dto.UserRequest;
import com.kid.A0.dto.UserResponse;
import com.kid.A0.model.Role;
import com.kid.A0.model.User;
import com.kid.A0.repo.UserRepo;
import com.kid.A0.service.Interface.UserServiceInterface;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class UserService implements UserServiceInterface {

    private static final String USER_CACHE = "user:v3";

    private final UserRepo userRepo;
    private final ModelMapper modelMapper;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public UserService(UserRepo userRepo, ModelMapper modelMapper, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.userRepo = userRepo;
        this.modelMapper = modelMapper;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    public List<UserResponse> getUsers() {
        return userRepo.findAll().stream().map(UserResponse::new).toList();
    }

    @Cacheable(cacheNames = USER_CACHE, key = "#id")
    public UserResponse getUser(long id) {
        User user = userRepo.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        return new UserResponse(user);
    }

    @Transactional
    @CachePut(cacheNames = USER_CACHE, key = "#result.id()")
    public UserResponse createUser(UserRequest userRequest) {
        User user = modelMapper.map(userRequest, User.class);
        user.setPassword(bCryptPasswordEncoder.encode(userRequest.getPassword()));
        user.setRole(Role.CUSTOMER);
        User createUser = userRepo.save(user);
        return new UserResponse(createUser);
    }

    @Transactional
    @CacheEvict(cacheNames = USER_CACHE,key = "#id")
    public String deleteUser(long id) {
        User user = userRepo.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        userRepo.delete(user);
        return "User delete";
    }

    @Transactional
    @CachePut(cacheNames = USER_CACHE,key = "#targetId")
    public UserResponse updateUser(String username, long targetId, UserRequest userRequest) {

        User requested = userRepo.findUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdmin = Role.ADMIN.equals(requested.getRole());
        boolean isOwner = requested.getId().equals(targetId);

        User targetUser = isOwner ? requested : userRepo.findById(targetId)
                                                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!isAdmin && !isOwner) {
            throw new RuntimeException("Forbidden: you can not edit this user");
        }

        if (userRequest.getRole() != null) {
            if (!isAdmin) {
                throw new RuntimeException("Forbidden: only admin can change user role");
            }
            targetUser.setRole(userRequest.getRole());
        }
        if (userRequest.getUsername() != null) {
            if (!targetUser.getUsername().equals(userRequest.getUsername()) && userRepo.existsUserByUsername(userRequest.getUsername())) {
                throw new RuntimeException("User with that name exist");
            }
            targetUser.setUsername(userRequest.getUsername());
        }
        if (userRequest.getEmail() != null) {
            if (!targetUser.getEmail().equals(userRequest.getEmail()) && userRepo.existsUserByEmail((userRequest.getEmail()))) {
                throw new RuntimeException("User with that email exist");
            }
            targetUser.setEmail(userRequest.getEmail());
        }

        if (userRequest.getPassword() != null && !userRequest.getPassword().isBlank()) {
            targetUser.setPassword(bCryptPasswordEncoder.encode(userRequest.getPassword()));
        }

        userRepo.save(targetUser);
        return new UserResponse(targetUser);
    }

    @Cacheable(cacheNames = USER_CACHE,key = "#username")
    public UserResponse getMe(String username) {
        User user = userRepo.findUserByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User Not Found"));
        return new UserResponse(user);
    }
}
