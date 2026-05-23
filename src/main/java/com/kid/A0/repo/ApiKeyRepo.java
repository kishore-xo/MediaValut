package com.kid.A0.repo;

import com.kid.A0.model.ApiKey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiKeyRepo extends JpaRepository<ApiKey, Long> {
    Page<ApiKey> findByUserIdAndRevokedFalse(Long userId, Pageable pageable);

    Optional<ApiKey> findByUserIdAndId(Long userId, Long keyId);

    @Query("""
           SELECT k FROM ApiKey k 
           JOIN FETCH k.user u 
           JOIN FETCH u.subscription s 
           JOIN FETCH s.plan 
           WHERE k.hashedKey = :hashKey
           """)
    Optional<ApiKey> findByHashedKey(String hashKey);

    void deleteApiKeyByRevoked(boolean revoked);
}
