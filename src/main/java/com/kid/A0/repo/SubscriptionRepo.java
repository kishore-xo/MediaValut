package com.kid.A0.repo;

import com.kid.A0.model.SubscribeStatus;
import com.kid.A0.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionRepo extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByUserIdAndStatus(Long userId, SubscribeStatus subscribeStatus);
}
