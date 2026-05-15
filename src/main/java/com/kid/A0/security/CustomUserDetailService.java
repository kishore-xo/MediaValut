package com.kid.A0.security;

import com.kid.A0.model.User;
import com.kid.A0.repo.UserRepo;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailService implements UserDetailsService {
    private final UserRepo userRepo;

    public CustomUserDetailService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        User user = userRepo.findUserByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found"));

        return new CustomUserDetails(user);
    }
}
