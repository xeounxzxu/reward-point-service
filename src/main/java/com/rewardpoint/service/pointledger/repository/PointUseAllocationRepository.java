package com.rewardpoint.service.pointledger.repository;

import com.rewardpoint.service.pointledger.entity.PointUse;
import com.rewardpoint.service.pointledger.entity.PointUseAllocation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointUseAllocationRepository extends JpaRepository<PointUseAllocation, Long> {

    List<PointUseAllocation> findByPointUseOrderByAllocationIdAsc(PointUse pointUse);
}
