package com.kid.A0.repo;

import com.kid.A0.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.unit.DataSize;

import java.util.Optional;

@Repository
public interface PlanRepo extends JpaRepository<Plan, Long> {
    boolean existsPlanByName(String name);

    Optional<Plan> findByName(String name);

    void deleteByName(String name);

}
