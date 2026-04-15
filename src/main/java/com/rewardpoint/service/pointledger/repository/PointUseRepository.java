package com.rewardpoint.service.pointledger.repository;

import com.rewardpoint.service.pointledger.entity.PointUse;
import com.rewardpoint.service.pointledger.entity.PointTransaction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointUseRepository extends JpaRepository<PointUse, Long> {

    Optional<PointUse> findByTransaction(PointTransaction transaction);
}
