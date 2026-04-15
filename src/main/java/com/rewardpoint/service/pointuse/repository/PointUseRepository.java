package com.rewardpoint.service.pointuse.repository;

import com.rewardpoint.service.pointuse.entity.PointUse;
import com.rewardpoint.service.pointcore.entity.PointTransaction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointUseRepository extends JpaRepository<PointUse, Long> {

    Optional<PointUse> findByTransaction(PointTransaction transaction);
}
