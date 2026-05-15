package com.kid.A0.aspect;

import com.kid.A0.annotation.CheckUploadLimit;
import com.kid.A0.model.User;
import com.kid.A0.repo.MediaRepo;
import com.kid.A0.repo.UserRepo;
import com.kid.A0.security.CustomUserDetails;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class UploadLimitAspect {


    private final UserRepo userRepo;
    private final MediaRepo mediaRepo;

    public UploadLimitAspect(UserRepo userRepo, MediaRepo mediaRepo) {
        this.userRepo = userRepo;
        this.mediaRepo = mediaRepo;
    }

    @Around("@annotation(com.kid.A0.annotation.CheckUploadLimit) && @annotation(checkUploadLimit)")
    public Object mediaUploadLimit(ProceedingJoinPoint joinPoint, CheckUploadLimit checkUploadLimit) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("User Not Authenticated");
        }

        Object principal = auth.getPrincipal();

        assert principal != null;
        String username = ((CustomUserDetails) principal).getUsername();

        User user = userRepo.findByUsernameWithPlan(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found"));

        int allowedCount = user.getSubscription().getPlan().getMediaCount();
        String mediaType = checkUploadLimit.type();
        int count = mediaRepo.countByUserIdAndTypeAndIsDeletedFalse(user.getId(), mediaType);
        if (count >= allowedCount) {
            throw new RuntimeException("Limit exceeded! Your plan allows only " + allowedCount + " " + mediaType + "s.");
        }
        return joinPoint.proceed();
    }
}
