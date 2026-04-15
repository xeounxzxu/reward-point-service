package com.rewardpoint.service.pointcore.repository;

import com.rewardpoint.service.pointcore.entity.PointPolicy;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointPolicyRepository extends JpaRepository<PointPolicy, Long> {

    Optional<PointPolicy> findFirstByActiveTrueOrderByPolicyIdDesc();
}
