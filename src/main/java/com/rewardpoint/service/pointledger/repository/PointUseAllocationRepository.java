package com.rewardpoint.service.pointledger.repository;

import com.rewardpoint.service.pointledger.entity.PointUse;
import com.rewardpoint.service.pointledger.entity.PointUseAllocation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PointUseAllocationRepository extends JpaRepository<PointUseAllocation, Long> {

    @Query("""
            select a
            from PointUseAllocation a
            join fetch a.grant g
            join fetch g.transaction t
            where a.pointUse = :pointUse
            order by a.allocationId asc
            """)
    List<PointUseAllocation> findByPointUseOrderByAllocationIdAsc(PointUse pointUse);
}
