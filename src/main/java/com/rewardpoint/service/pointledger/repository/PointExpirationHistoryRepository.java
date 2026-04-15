package com.rewardpoint.service.pointledger.repository;

import com.rewardpoint.service.pointledger.entity.PointExpirationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointExpirationHistoryRepository extends JpaRepository<PointExpirationHistory, Long> {
}
