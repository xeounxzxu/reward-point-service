package com.rewardpoint.service.pointcore.repository;

import com.rewardpoint.service.pointcore.entity.PointTransaction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    Optional<PointTransaction> findByTransactionKey(String transactionKey);
}
