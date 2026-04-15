package com.rewardpoint.service.pointledger.repository;

import com.rewardpoint.service.pointledger.entity.PointPolicy;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointPolicyRepository extends JpaRepository<PointPolicy, Long> {

    Optional<PointPolicy> findFirstByActiveTrueOrderByPolicyIdDesc();
}
