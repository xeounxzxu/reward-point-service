package com.rewardpoint.service.pointearn.repository;

import com.rewardpoint.service.pointearn.entity.PointExpirationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointExpirationHistoryRepository extends JpaRepository<PointExpirationHistory, Long> {
}
