package com.kid.A0.repo;

import com.kid.A0.model.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User, Long> {
    Optional<User> findUserByUsername(String username);

    @Query("""
           SELECT u FROM User u 
           LEFT JOIN FETCH u.subscription s 
           LEFT JOIN FETCH s.plan 
           WHERE u.username = :username
           """)
    Optional<User> findByUsernameWithPlan(@Param("username") String username);

    boolean existsUserByUsername(String username);

    boolean existsUserByEmail(String email);

    boolean existsUserById(Long id);

    @Query("""
           SELECT u FROM User u 
           LEFT JOIN FETCH u.subscription s 
           LEFT JOIN FETCH s.plan 
           WHERE u.id = :id
           """)
    Optional<User> findByIdWithPlan(@Param("id") Long id);
}
