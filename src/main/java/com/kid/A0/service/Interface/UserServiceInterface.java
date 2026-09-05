package com.kid.A0.service.Interface;

import com.kid.A0.dto.UserRequest;
import com.kid.A0.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserServiceInterface {

    Page<UserResponse> getUsers(Pageable pageable);

    UserResponse getUser(long id);

    UserResponse createUser(UserRequest userRequest);

    String deleteUser(long id);

    UserResponse updateUser(String username, long targetId, UserRequest userRequest);

    UserResponse getMe(String username);
}